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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.utils.Localization;

@NonNullByDefault
public class MyLightPowerCounterThingHandler extends MyLightBasePowerCounterThingHandler {
    protected @NonNullByDefault({}) final Logger logger = LoggerFactory
            .getLogger(MyLightPowerCounterThingHandler.class);

    public MyLightPowerCounterThingHandler(Thing thing, Localization localization,
            final TimeZoneProvider timeZoneProvider) {
        super(thing, localization, timeZoneProvider);

        logger.trace("MyLightPowerCounterThingHandler(thing={},localization={}) constructor called.", thing,
                localization);
    }

    @Override
    protected List<String> getExpectedTypeIds() {
        return List.of("production-counter", "asoka_electric_counter");
    }
}
