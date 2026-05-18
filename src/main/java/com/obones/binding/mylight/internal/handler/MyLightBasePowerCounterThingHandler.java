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

import com.obones.binding.mylight.internal.utils.Localization;

@NonNullByDefault
public abstract class MyLightBasePowerCounterThingHandler extends MyLightBaseThingHandler {
    protected @NonNullByDefault({}) final Logger logger = LoggerFactory
            .getLogger(MyLightBasePowerCounterThingHandler.class);

    public MyLightBasePowerCounterThingHandler(Thing thing, Localization localization,
            final TimeZoneProvider timeZoneProvider) {
        super(thing, localization, timeZoneProvider);

        logger.trace("MyLightBasePowerCounterThingHandler(thing={},localization={}) constructor called.", thing,
                localization);
    }

    /**
     * Updates the channel with the given UID from the latest MyLight data retrieved.
     *
     * @param channelUID UID of the channel
     */
    @Override
    protected void updateChannel(ChannelUID channelUID) {
        logger.debug("MyLightPowerCounterThingHandler: updateChannel {}", channelUID);

        switch (channelUID.getId()) {
            case CHANNEL_BASE_POWER_COUNTER_ELECTRIC_POWER:
                var powerSensorState = getSensorStateBySuffix("-pow");
                if (powerSensorState != null)
                    updateState(channelUID,
                            getQuantityTypeState(powerSensorState.measure.value, getUnit(powerSensorState)));
                break;
            default:
                logger.error("Unknown channel {}", channelUID.toString());
        }
    }
}
