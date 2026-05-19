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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.config.MyLightWaterHeaterThingConfiguration;
import com.obones.binding.mylight.internal.connection.api.MyLightRoomDevice;
import com.obones.binding.mylight.internal.utils.Localization;

@NonNullByDefault
public class MyLightWaterHeaterThingHandler extends MyLightBasePowerCounterThingHandler {
    protected @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightWaterHeaterThingHandler.class);

    public MyLightWaterHeaterThingHandler(Thing thing, Localization localization,
            final TimeZoneProvider timeZoneProvider) {
        super(thing, localization, timeZoneProvider);

        logger.trace("MyLightWaterHeaterThingHandler(thing={},localization={}) constructor called.", thing,
                localization);
    }

    @Override
    protected List<String> getExpectedTypeIds() {
        return List.of("water_heater");
    }

    @Override
    protected void ensureValidDevice(MyLightRoomDevice device) throws IllegalArgumentException {
        super.ensureValidDevice(device);

        if (!device.has_actuator)
            throw new IllegalArgumentException(
                    String.format("Wrong has_actuator for %s, expected true, got false", device.device_id));
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
            case CHANNEL_WATER_HEATER_POWER:
                var powerActuatorState = getActuatorStateBySuffix("-sw");
                if (powerActuatorState != null)
                    updateState(channelUID, getOnOffState(powerActuatorState.state.is_on));
                break;
            default:
                super.updateChannel(channelUID);
        }
    }

    @Override
    protected boolean handleActionCommand(BridgeHandler bridgeHandler, ChannelUID channelUID, Command command) {
        var config = getConfigAs(MyLightWaterHeaterThingConfiguration.class);

        switch (channelUID.getId()) {
            case CHANNEL_WATER_HEATER_POWER:
                ((MyLightBridgeHandler) bridgeHandler).setRelayState(config.deviceId, command);
                return true;
        }
        return super.handleActionCommand(bridgeHandler, channelUID, command);
    }
}
