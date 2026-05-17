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
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.config.MyLightSmartBatteryThingConfiguration;
import com.obones.binding.mylight.internal.connection.api.MyLightRoomDevice;
import com.obones.binding.mylight.internal.connection.api.MyLightSensorState;
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

    private double batteryCapacity = 0;

    public MyLightSmartBatteryThingHandler(Thing thing, Localization localization,
            final TimeZoneProvider timeZoneProvider) {
        super(thing, localization, timeZoneProvider);
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

    private @Nullable MyLightSensorState getSensorStateBySuffix(String suffix) {
        if (deviceState != null) {
            for (var sensorState : deviceState.sensorStates) {
                if (sensorState.sensorId.endsWith(suffix))
                    return sensorState;
            }
        }

        return null;
    }

    @Override
    protected String getExpectedTypeId() {
        return "my_smart_battery";
    }

    @Override
    protected void updateDeviceProperties(MyLightRoomDevice device) {
        batteryCapacity = device.batteryCapacity;
        thing.setProperty(PROPERTY_SMART_BATTERY_SUBSCRIBED_CAPACITY, Double.toString(batteryCapacity));
    }

    /**
     * Updates the channel with the given UID from the latest MyLight data retrieved.
     *
     * @param channelUID UID of the channel
     */
    protected void updateChannel(ChannelUID channelUID) {
        logger.debug("MyLightSmartBatteryThingHandler: updateChannel {}", channelUID);

        var stateOfChargeSensorState = getSensorStateBySuffix("-soc");

        switch (channelUID.getId()) {
            case CHANNEL_SMART_BATTERY_CHARGE_LEVEL:
                if (stateOfChargeSensorState != null) {
                    double stateOfCharge = stateOfChargeSensorState.measure.value;
                    double maxStateOfCharge = 36e5 * batteryCapacity;
                    double boundedStateOfCharge = Math.min(stateOfCharge, maxStateOfCharge);
                    double chargeLevel = (boundedStateOfCharge == 0) ? 0
                            : Math.min(100, boundedStateOfCharge / 36e5 * 100 / batteryCapacity);

                    updateState(channelUID, getDecimalTypeState(chargeLevel));
                    return;
                }
                break;
            case CHANNEL_SMART_BATTERY_CHARGE_ENERGY:
                if (stateOfChargeSensorState != null) {
                    double stateOfCharge = stateOfChargeSensorState.measure.value;
                    double maxStateOfCharge = 36e5 * batteryCapacity;
                    double boundedStateOfCharge = Math.min(stateOfCharge, maxStateOfCharge);

                    updateState(channelUID, getQuantityTypeState(boundedStateOfCharge, Units.WATT_SECOND));
                    return;
                }
                break;
            case CHANNEL_SMART_BATTERY_INSTANTANEOUS_CHARGE_ENERGY:
                var instantaneousChargeEnergySensorState = getSensorStateBySuffix("charge_energy");
                if (instantaneousChargeEnergySensorState != null) {
                    updateState(channelUID, getQuantityTypeState(instantaneousChargeEnergySensorState.measure.value,
                            Units.WATT_SECOND));
                }
                break;
            case CHANNEL_SMART_BATTERY_INSTANTANEOUS_DISCHARGE_ENERGY:
                var instantaneousDischargeEnergySensorState = getSensorStateBySuffix("discharge_energy");
                if (instantaneousDischargeEnergySensorState != null) {
                    updateState(channelUID, getQuantityTypeState(instantaneousDischargeEnergySensorState.measure.value,
                            Units.WATT_SECOND));
                }
                break;
            case CHANNEL_SMART_BATTERY_INSTANTANEOUS_LOSS_ENERGY:
                var instantaneousLossEnergySensorState = getSensorStateBySuffix("loss_energy");
                if (instantaneousLossEnergySensorState != null) {
                    updateState(channelUID,
                            getQuantityTypeState(instantaneousLossEnergySensorState.measure.value, Units.WATT_SECOND));
                }
                break;
            default:
                logger.error("Unknown channel {}", channelUID.toString());
        }
    }
}
