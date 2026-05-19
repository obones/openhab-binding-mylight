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
package com.obones.binding.mylight.internal.console;

import static com.obones.binding.mylight.internal.MyLightBindingConstants.*;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.obones.binding.mylight.internal.handler.MyLightBridgeHandler;

@NonNullByDefault
@Component(service = ConsoleCommandExtension.class)
public class MyLightCommandExtension extends AbstractConsoleCommandExtension {
    private ThingRegistry thingRegistry;

    public static final String BRIDGES_COMMAND = "bridges";
    public static final String DEVICES_COMMAND = "devices";

    @Activate
    public MyLightCommandExtension(final @Reference ThingRegistry thingRegistry) {
        super("mylight", "Interact with the MyLight binding.");
        this.thingRegistry = thingRegistry;
    }

    @Override
    public void execute(String[] arguments, Console console) {
        switch (arguments.length) {
            case 1:
                if (arguments[0].equals(BRIDGES_COMMAND)) {
                    handleBridgesCommand(console);
                    return;
                }
                break;
            case 2:
                if (arguments[1].equals(DEVICES_COMMAND)) {
                    handleDevicesCommand(arguments[0], console);
                    return;
                }
        }
        printUsage(console);
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(new String[] { //
                buildCommandUsage(BRIDGES_COMMAND, "show the known bridges"),
                buildCommandUsage("<bridgeUID> " + DEVICES_COMMAND, "show the devices known to the given bridge") });
    }

    private void handleBridgesCommand(Console console) {
        var things = thingRegistry.getAll();
        for (var thing : things) {
            if (thing.getThingTypeUID().equals(THING_TYPE_BRIDGE))
                console.println(thing.getUID().getAsString());
        }
    }

    private void handleDevicesCommand(String bridgeUID, Console console) {
        var bridge = getThing(bridgeUID);
        if (bridge == null) {
            console.println(String.format("%s is not a valid thing UID", bridgeUID));
            return;
        }

        var thingHandler = bridge.getHandler();
        if (thingHandler instanceof MyLightBridgeHandler bridgeHandler) {
            if (bridge.getStatusInfo().getStatus() != ThingStatus.ONLINE) {
                console.println(
                        String.format("The %s MyLight bridge is not online, please verify its status.", bridgeUID));
                return;
            }

            var rooms = bridgeHandler.getRooms();
            if (rooms == null) {
                console.println(
                        String.format("No rooms found for bridge %s, make sure it has updated once.", bridgeUID));
                return;
            }

            console.println(
                    "*" + "-".repeat(32) + "*" + "-".repeat(27) + "*" + "-".repeat(27) + "*" + "-".repeat(14) + "*");
            console.println(
                    String.format("| %-30s | %-25s | %-25s | %s |", "Name", "Device Id", "Type", "Has actuator"));
            console.println(
                    "*" + "-".repeat(32) + "*" + "-".repeat(27) + "*" + "-".repeat(27) + "*" + "-".repeat(14) + "*");
            for (var room : rooms) {
                for (var device : room.devices) {
                    console.println(String.format("| %-30.30s | %-25.25s | %-25.25s | %-12.12b |", device.name,
                            device.device_id, device.type_id, device.has_actuator));
                }
            }
            console.println(
                    "*" + "-".repeat(32) + "*" + "-".repeat(27) + "*" + "-".repeat(27) + "*" + "-".repeat(14) + "*");
        } else {
            console.println(String.format("%s is not a MyLightBridge UID", bridgeUID));
            return;
        }
    }

    private @Nullable Thing getThing(String uid) {
        Thing thing = null;
        try {
            ThingUID thingUID = new ThingUID(uid);
            thing = thingRegistry.get(thingUID);
        } catch (IllegalArgumentException e) {
            thing = null;
        }
        return thing;
    }
}
