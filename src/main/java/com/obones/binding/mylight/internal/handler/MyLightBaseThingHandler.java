/**
 * Copyright (c) 2026 Olivier Sannier
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package com.obones.binding.mylight.internal.handler;

import static com.obones.binding.mylight.internal.MyLightBindingConstants.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Map.Entry;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.obones.binding.mylight.internal.config.MyLightBaseThingConfiguration;
import com.obones.binding.mylight.internal.connection.MyLightRoomsApiResponse;
import com.obones.binding.mylight.internal.connection.MyLightStatesApiResponse;
import com.obones.binding.mylight.internal.connection.api.MyLightRoomDevice;
import com.obones.binding.mylight.internal.connection.api.MyLightSensorState;
import com.obones.binding.mylight.internal.connection.api.MyLightState;
import com.obones.binding.mylight.internal.utils.Localization;

/***
 * The{@link MyLightBaseThingHandler} is the base class for all MyLight thing handlers
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public abstract class MyLightBaseThingHandler extends BaseThingHandler {

    protected @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightBridgeHandler.class);
    protected final TimeZoneProvider timeZoneProvider;
    protected static final Gson gson = new Gson();
    protected @Nullable MyLightState deviceState;

    public Localization localization;

    public MyLightBaseThingHandler(Thing thing, Localization localization, final TimeZoneProvider timeZoneProvider) {
        super(thing);
        this.localization = localization;
        this.timeZoneProvider = timeZoneProvider;
        logger.trace("MyLightSmartBatteryHandler(thing={},localization={}) constructor called.", thing, localization);
    }

    @Override
    public void initialize() {
        logger.trace("initialize() called.");
        Bridge thisBridge = getBridge();
        logger.debug("initialize(): Initializing thing {} in combination with bridge {}.", getThing().getUID(),
                thisBridge);

        // Initialize the channels early on as they don't require the bridge to be present
        // This allows seeing the effect of the various configuration switches without needing
        // to activate the bridge
        // do not call initializeChannels();

        if (thisBridge == null) {
            logger.trace("initialize() updating ThingStatus to OFFLINE/CONFIGURATION_PENDING.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING);

        } else if (thisBridge.getStatus() == ThingStatus.ONLINE) {
            logger.trace("initialize() checking for configuration validity.");

            boolean configValid = validateConfig();

            if (configValid) {
                logger.trace("initialize() updating ThingStatus to ONLINE.");
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            logger.trace("initialize() updating ThingStatus to OFFLINE/BRIDGE_OFFLINE.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        }
        logger.trace("initialize() done.");
    }

    protected synchronized boolean validateConfig() {
        MyLightBaseThingConfiguration config = getConfigAs(MyLightBaseThingConfiguration.class);

        return config.deviceId != "";
    }

    @Override
    public void dispose() {
        logger.trace("dispose() called.");
        super.dispose();
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        logger.trace("channelLinked({}) called.", channelUID.getAsString());

        if (thing.getStatus() == ThingStatus.ONLINE) {
            handleCommand(channelUID, RefreshType.REFRESH);
        }
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        if (isInitialized()) { // prevents change of address
            validateConfigurationParameters(configurationParameters);
            Configuration configuration = editConfiguration();
            for (Entry<String, Object> configurationParameter : configurationParameters.entrySet()) {
                logger.trace("handleConfigurationUpdate(): found modified config entry {}.",
                        configurationParameter.getKey());
                configuration.put(configurationParameter.getKey(), configurationParameter.getValue());
            }
            // persist new configuration and reinitialize handler
            dispose();
            updateConfiguration(configuration);
            initialize();
        } else {
            super.handleConfigurationUpdate(configurationParameters);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo info) {
        switch (info.getStatus()) {
            case OFFLINE:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                break;

            case ONLINE:
                initialize();
                break;

            default:
                super.bridgeStatusChanged(info);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand({},{}) initiated by {}.", channelUID.getAsString(), command,
                Thread.currentThread());
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.trace("handleCommand() nothing yet to do as there is no bridge available.");
        } else {
            BridgeHandler handler = bridge.getHandler();
            if (handler == null) {
                logger.trace("handleCommand() nothing yet to do as thing is not initialized.");
            } else {
                if (command instanceof RefreshType) {
                    updateChannel(channelUID);
                } else {
                    logger.debug("The MyLight binding is a read-only binding and cannot handle command '{}'.", command);
                }
            }
        }
    }

    /**
     * Updates MyLight data for this location.
     */
    public void updateData(MyLightStatesApiResponse states) {
        if (storeDeviceState(states)) {
            updateChannels();
            updateStatus(ThingStatus.ONLINE);
        }
    }

    protected abstract String getExpectedTypeId();

    protected void ensureValidDevice(MyLightRoomDevice device) throws IllegalArgumentException {
        var expectedTypeId = getExpectedTypeId();
        if (!device.type_id.equals(expectedTypeId)) {
            throw new IllegalArgumentException(String.format("Wrong type id for {}: expected {}, got {}",
                    getThing().getUID(), expectedTypeId, device.type_id));
        }
    }

    protected void updateDeviceProperties(MyLightRoomDevice device) {
    }

    public void updateDeviceProperties(MyLightRoomsApiResponse rooms) {
        logger.debug("Update device properties of thing '{}'.", getThing().getUID());

        MyLightBaseThingConfiguration config = getConfigAs(MyLightBaseThingConfiguration.class);

        for (var room : rooms) {
            for (var device : room.devices) {
                if (device.device_id.equals(config.deviceId)) {
                    try {
                        ensureValidDevice(device);
                        updateDeviceProperties(device);
                    } catch (IllegalArgumentException e) {
                        logger.error(e.getMessage());
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
                    }
                    break;
                }
            }
        }
    }

    /**
     * Stores the data from MyLight API.
     *
     * @param states {@link MyLightStatesApiResponse} instance
     * @return true, if the device state was found from given states
     */
    protected boolean storeDeviceState(MyLightStatesApiResponse states) {
        logger.debug("Update data of thing '{}'.", getThing().getUID());

        MyLightBaseThingConfiguration config = getConfigAs(MyLightBaseThingConfiguration.class);

        boolean result = false;
        for (var state : states) {
            if (state.deviceId.equals(config.deviceId)) {
                deviceState = state;
                result = true;
                break;
            }
        }

        var now = OffsetDateTime.now(ZoneOffset.UTC).withNano(0);
        thing.setProperty(PROPERTY_THING_LAST_UPDATED, DateTimeFormatter.ISO_DATE_TIME.format(now));

        return result;
    }

    /**
     * Updates all channels of this handler from the latest retrieved MyLight data.
     */
    private void updateChannels() {
        for (Channel channel : getThing().getChannels()) {
            ChannelUID channelUID = channel.getUID();
            if (ChannelKind.STATE.equals(channel.getKind()) && isLinked(channelUID)) {
                try {
                    updateChannel(channelUID);
                } catch (IllegalArgumentException e) {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            String.format("Channel {} : {}", channelUID.getAsString(), e.getMessage()));
                }
            }
        }
    }

    /**
     * Updates the channel with the given UID from the latest retrieved MyLight data.
     *
     * @param channelUID UID of the channel
     */
    protected abstract void updateChannel(ChannelUID channelUID);

    protected @Nullable MyLightSensorState getSensorStateBySuffix(String suffix) {
        var deviceState = this.deviceState;
        if (deviceState != null) {
            for (var sensorState : deviceState.sensorStates) {
                if (sensorState.sensorId.endsWith(suffix))
                    return sensorState;
            }
        }

        return null;
    }

    protected Unit<?> getUnit(MyLightSensorState sensorState) throws IllegalArgumentException {
        switch (sensorState.measure.unit) {
            case "Ws":
                return Units.WATT_SECOND;
            case "watt":
            case "W":
                return Units.WATT;
            default:
                throw new IllegalArgumentException(String.format("Unsupported unit: {}", sensorState.measure.unit));
        }
    }

    protected State getDecimalTypeState(@Nullable Number value) {
        return ((value == null || !Double.isFinite(value.doubleValue()))) ? UnDefType.UNDEF : new DecimalType(value);
    }

    protected State getQuantityTypeState(@Nullable Number value, Unit<?> unit) {
        return ((value == null) || !Double.isFinite(value.doubleValue())) ? UnDefType.UNDEF
                : new QuantityType<>(value, unit);
    }

    protected State getQuantityTypeState(@Nullable Float value, int multiplier, Unit<?> unit) {
        return ((value == null || !Float.isFinite(value))) ? UnDefType.UNDEF
                : new QuantityType<>(value * multiplier, unit);
    }

    protected State getDateTimeTypeState(@Nullable Long value) {
        return (value == null) ? UnDefType.UNDEF
                : new DateTimeType(ZonedDateTime.ofInstant(Instant.ofEpochSecond(value.longValue()),
                        timeZoneProvider.getTimeZone()));
    }

    protected State getOnOffState(@Nullable Float value) {
        return (value == null) ? UnDefType.UNDEF : (value == 1) ? OnOffType.ON : OnOffType.OFF;
    }
}
