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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.CommunicationException;
import org.openhab.core.i18n.ConfigurationException;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandlerCallback;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.config.MyLightSmartBatteryThingConfiguration;
import com.obones.binding.mylight.internal.connection.MyLightConnection;
import com.obones.binding.mylight.internal.connection.MyLightStatesApiResponse;
import com.obones.binding.mylight.internal.utils.Localization;

/***
 * The{@link MyLightSmartBatteryThingHandler} is responsible for updating weather forecast related channels, which are
 * retrieved via {@link MyLightBridgeHandler}.
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public class MyLightSmartBatteryThingHandler extends MyLightBaseThingHandler {
    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightBridgeHandler.class);

    @Nullable
    private String batteryId = null;
    private double batteryCapacity = 0;
    @Nullable
    private MyLightStatesApiResponse states = null;

    public MyLightSmartBatteryThingHandler(Thing thing, Localization localization,
            final TimeZoneProvider timeZoneProvider, ChannelTypeRegistry channelTypeRegistry) {
        super(thing, localization, timeZoneProvider, channelTypeRegistry);
        logger.trace("MyLightSmartBatteryHandler(thing={},localization={}) constructor called.", thing, localization);
    }

    @Override
    protected synchronized boolean validateConfig() {
        boolean result = super.validateConfig();

        if (result) {
            MyLightSmartBatteryThingConfiguration config = getConfigAs(MyLightSmartBatteryThingConfiguration.class);

        }

        return result;
    }

    protected boolean refreshData(MyLightConnection connection, String authToken)
            throws CommunicationException, ConfigurationException {
        MyLightSmartBatteryThingConfiguration config = getConfigAs(MyLightSmartBatteryThingConfiguration.class);

        if (batteryId == null) {
            var rooms = connection.getRooms(authToken);
            for (var room : rooms) {
                for (var device : room.devices) {
                    if (device.type_id.equals("my_smart_battery")) {
                        batteryId = device.device_id;
                        batteryCapacity = device.batteryCapacity;
                    }
                }
            }
        }

        if (batteryId != null) {
            states = connection.getStates(authToken);
            return true;
        }

        return false;
    }

    /**
     * Updates the channel with the given UID from the latest MyLight data retrieved.
     *
     * @param channelUID UID of the channel
     */
    protected void updateChannel(ChannelUID channelUID) {
        logger.debug("MyLightSmartBatteryThingHandler: updateChannel {}", channelUID);

        switch (channelUID.getId()) {
            case CHANNEL_SMART_BATTERY_CHARGE_LEVEL:
                for (var state : states) {
                    if (state.deviceId.equals(batteryId)) {
                        for (var sensorState : state.sensorStates) {
                            if (sensorState.sensorId.endsWith("-soc")) {
                                double stateOfCharge = sensorState.measure.value;
                                double maxStateOfCharge = 36e5 * batteryCapacity;
                                double boundedStateOfCharge = Math.min(stateOfCharge, maxStateOfCharge);
                                double chargeLevel = (boundedStateOfCharge == 0) ? 0
                                        : Math.min(100, boundedStateOfCharge / 36e5 * 100 / batteryCapacity);

                                updateState(channelUID, new DecimalType(chargeLevel));
                                return;
                            }
                        }
                    }
                }
                break;
        }
    }

    protected void initializeChannels(ThingHandlerCallback callback, ThingBuilder builder, ThingUID thingUID) {
    }
}
