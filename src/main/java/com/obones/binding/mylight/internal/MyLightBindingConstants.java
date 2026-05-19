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
package com.obones.binding.mylight.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link MyLightBindingConstants} class defines common constants, which are
 * used across the whole binding.
 * <P>
 * This class contains the Thing identifications.
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public class MyLightBindingConstants {
    /** Basic binding identification. */
    public static final String BINDING_ID = "mylight";

    /**
     * The Thing identification of the <B>MyLight</B> bridge.
     */
    private static final String THING_MYLIGHT_BRIDGE = "mylight";
    /**
     * The Thing identification of a smart battery defined on the <B>MyLight</B> bridge.
     */
    private static final String THING_MYLIGHT_SMART_BATTERY = "smart-battery";
    /**
     * The Thing identification of the virtual counters defined on the <B>MyLight</B> bridge.
     */
    private static final String THING_MYLIGHT_VIRTUAL_COUNTERS = "virtual-counters";
    /**
     * The Thing identification of the power counters defined on the <B>MyLight</B> bridge.
     */
    private static final String THING_MYLIGHT_POWER_COUNTER = "power-counter";
    /**
     * The Thing identification of the water heater relays defined on the <B>MyLight</B> bridge.
     */
    private static final String THING_MYLIGHT_WATER_HEATER = "water-heater";

    // Discovered things id
    public static final String SYSTEM_LOCATION_THING_ID = "system";

    // List of all Bridge Type UIDs
    public static final ThingTypeUID THING_TYPE_BRIDGE = new ThingTypeUID(BINDING_ID, THING_MYLIGHT_BRIDGE);

    // List of all Thing Type UIDs beyond the bridge(s)
    public static final ThingTypeUID THING_TYPE_MYLIGHT_SMART_BATTERY = new ThingTypeUID(BINDING_ID,
            THING_MYLIGHT_SMART_BATTERY);
    public static final ThingTypeUID THING_TYPE_MYLIGHT_VIRTUAL_COUNTERS = new ThingTypeUID(BINDING_ID,
            THING_MYLIGHT_VIRTUAL_COUNTERS);
    public static final ThingTypeUID THING_TYPE_MYLIGHT_POWER_COUNTER = new ThingTypeUID(BINDING_ID,
            THING_MYLIGHT_POWER_COUNTER);
    public static final ThingTypeUID THING_TYPE_MYLIGHT_WATER_HEATER = new ThingTypeUID(BINDING_ID,
            THING_MYLIGHT_WATER_HEATER);

    // Definitions of different set of Things
    public static final Set<ThingTypeUID> SUPPORTED_THINGS_BRIDGE = new HashSet<>(Arrays.asList(THING_TYPE_BRIDGE));
    public static final Set<ThingTypeUID> SUPPORTED_THINGS_ITEMS = new HashSet<>(
            Arrays.asList(THING_TYPE_MYLIGHT_SMART_BATTERY, THING_TYPE_MYLIGHT_VIRTUAL_COUNTERS,
                    THING_TYPE_MYLIGHT_POWER_COUNTER, THING_TYPE_MYLIGHT_WATER_HEATER));

    // List of all channel ids
    public static final String CHANNEL_SMART_BATTERY_CHARGE_LEVEL = "charge-level";
    public static final String CHANNEL_SMART_BATTERY_CHARGE_ENERGY = "charge-energy";
    public static final String CHANNEL_SMART_BATTERY_INSTANTANEOUS_CHARGE_ENERGY = "instantaneous-charge-energy";
    public static final String CHANNEL_SMART_BATTERY_INSTANTANEOUS_DISCHARGE_ENERGY = "instantaneous-discharge-energy";
    public static final String CHANNEL_SMART_BATTERY_INSTANTANEOUS_LOSS_ENERGY = "instantaneous-loss-energy";

    public static final String CHANNEL_VIRTUAL_COUNTERS_PRODUCED_ENERGY = "produced-energy";
    public static final String CHANNEL_VIRTUAL_COUNTERS_ELECTRICITY_METER_ENERGY = "electricity-meter-energy";
    public static final String CHANNEL_VIRTUAL_COUNTERS_TOTAL_ENERGY = "total-energy";
    public static final String CHANNEL_VIRTUAL_COUNTERS_GRID_ENERGY = "grid-energy";
    public static final String CHANNEL_VIRTUAL_COUNTERS_GREEN_ENERGY = "green-energy";
    public static final String CHANNEL_VIRTUAL_COUNTERS_AUTONOMY_RATE = "autonomy-rate";
    public static final String CHANNEL_VIRTUAL_COUNTERS_SELF_CONSUMPTION = "self-consumption";

    public static final String CHANNEL_BASE_POWER_COUNTER_ELECTRIC_POWER = "electric-power";

    public static final String CHANNEL_WATER_HEATER_POWER = "on-off";

    /** Channel/Property identifier describing the current Bridge State. */
    public static final String PROPERTY_BRIDGE_API_VERSION = "apiVersion";

    // Thing properties
    public static final String PROPERTY_THING_LAST_UPDATED = "last-updated";

    // not "device-id" for it to match the config parameter
    public static final String PROPERTY_THING_DEVICE_ID = "deviceId";

    // SmartBattery thing properties
    public static final String PROPERTY_SMART_BATTERY_SUBSCRIBED_CAPACITY = "subscribed-capacity";

    // List of all bridge channel/ids
    public static final String CHANNEL_BRIDGE_LAST_UPDATED = "last-updated";
}
