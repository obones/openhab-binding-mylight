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
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.config.MyLightSmartBatteryThingConfiguration;
import com.obones.binding.mylight.internal.utils.Localization;

/***
 * The{@link MyLightVirtualCountersThingHandler} is responsible for updating virtual counters related channels, which
 * are retrieved via {@link MyLightBridgeHandler}.
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public class MyLightVirtualCountersThingHandler extends MyLightBaseThingHandler {
    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightBridgeHandler.class);

    public MyLightVirtualCountersThingHandler(Thing thing, Localization localization,
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

    @Override
    protected String getExpectedTypeId() {
        return "virtual";
    }

    /**
     * Updates the channel with the given UID from the latest MyLight data retrieved.
     *
     * @param channelUID UID of the channel
     */
    protected void updateChannel(ChannelUID channelUID) {
        logger.debug("MyLightSmartBatteryThingHandler: updateChannel {}", channelUID);

        switch (channelUID.getId()) {
            case CHANNEL_VIRTUAL_COUNTERS_PRODUCED_ENERGY:
                var producedEnergySensorState = getSensorStateBySuffix("-produced_energy");
                if (producedEnergySensorState != null)
                    updateState(channelUID, getQuantityTypeState(producedEnergySensorState.measure.value,
                            getUnit(producedEnergySensorState)));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_ELECTRICITY_METER_ENERGY:
                var electricityMeterEnergySensorState = getSensorStateBySuffix("-electricity_meter_energy");
                if (electricityMeterEnergySensorState != null)
                    updateState(channelUID, getQuantityTypeState(electricityMeterEnergySensorState.measure.value,
                            getUnit(electricityMeterEnergySensorState)));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_TOTAL_ENERGY:
                var totalEnergySensorState = getSensorStateBySuffix("-total_energy");
                if (totalEnergySensorState != null)
                    updateState(channelUID, getQuantityTypeState(totalEnergySensorState.measure.value,
                            getUnit(totalEnergySensorState)));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_GRID_ENERGY:
                var gridEnergySensorState = getSensorStateBySuffix("-grid_energy");
                if (gridEnergySensorState != null)
                    updateState(channelUID,
                            getQuantityTypeState(gridEnergySensorState.measure.value, getUnit(gridEnergySensorState)));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_GREEN_ENERGY:
                var greenEnergySensorState = getSensorStateBySuffix("-green_energy");
                if (greenEnergySensorState != null)
                    updateState(channelUID, getQuantityTypeState(greenEnergySensorState.measure.value,
                            getUnit(greenEnergySensorState)));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_AUTONOMY_RATE:
                var autonomyRateEnergySensorState = getSensorStateBySuffix("-autonomy_rate");
                if (autonomyRateEnergySensorState != null)
                    updateState(channelUID, getDecimalTypeState(autonomyRateEnergySensorState.measure.value));
                break;
            case CHANNEL_VIRTUAL_COUNTERS_SELF_CONSUMPTION:
                var selfConsumptionEnergySensorState = getSensorStateBySuffix("-selfconso");
                if (selfConsumptionEnergySensorState != null)
                    updateState(channelUID, getDecimalTypeState(selfConsumptionEnergySensorState.measure.value));
                break;
            default:
                logger.error("Unknown channel {}", channelUID.toString());
        }
    }
}
