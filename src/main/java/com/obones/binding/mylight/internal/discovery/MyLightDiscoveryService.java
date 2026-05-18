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
/**
 *
 * The {@link MyLightDiscoveryService} creates things based on the configured location.
 *
 * @author Olivier Sannier - Initial contribution
 */
package com.obones.binding.mylight.internal.discovery;

import static com.obones.binding.mylight.internal.MyLightBindingConstants.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.handler.MyLightBridgeHandler;

/**
 * The {@link MyLightDiscoveryService} is responsible for discovering the system location as defined in global
 * options.
 *
 * @author Olivier Sannier - Initial contribution.
 */
@NonNullByDefault
public class MyLightDiscoveryService extends AbstractDiscoveryService {
    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightDiscoveryService.class);

    private static final int DISCOVERY_TIMEOUT_SECONDS = 2;
    private static final int DISCOVERY_INTERVAL_SECONDS = 60;
    private @Nullable ScheduledFuture<?> discoveryJob;

    private final MyLightBridgeHandler bridgeHandler;

    /**
     * Creates an MyLightDiscoveryService.
     */
    public MyLightDiscoveryService(MyLightBridgeHandler bridgeHandler, LocaleProvider localeProvider,
            TranslationProvider i18nProvider) {
        super(SUPPORTED_THINGS_ITEMS, DISCOVERY_TIMEOUT_SECONDS);
        this.bridgeHandler = bridgeHandler;
        this.localeProvider = localeProvider;
        this.i18nProvider = i18nProvider;
        activate(null);
    }

    @Override
    protected void activate(@Nullable Map<String, Object> configProperties) {
        super.activate(configProperties);
    }

    @Override
    public void deactivate() {
        logger.debug("Removing older discovery services.");
        removeOlderResults(Instant.now(), bridgeHandler.getThing().getUID());
        super.deactivate();
    }

    @Override
    protected void startScan() {
        logger.debug("Start manual MyLight devices discovery scan.");
        scanForNewDevices();
    }

    @Override
    protected synchronized void stopScan() {
        logger.debug("Stop manual MyLight devices discovery scan.");
        super.stopScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob == null || localDiscoveryJob.isCancelled()) {
            logger.debug("Start MyLight devices background discovery job at interval {} s.",
                    DISCOVERY_INTERVAL_SECONDS);
            localDiscoveryJob = scheduler.scheduleWithFixedDelay(() -> {
                scanForNewDevices();
            }, 0, DISCOVERY_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> localDiscoveryJob = discoveryJob;
        if (localDiscoveryJob != null && !localDiscoveryJob.isCancelled()) {
            logger.debug("Stop MyLight devices background discovery job.");
            if (localDiscoveryJob.cancel(true)) {
                discoveryJob = null;
            }
        }
    }

    private void scanForNewDevices() {
        ThingUID bridgeUID = bridgeHandler.getThing().getUID();
        logger.debug("Creating results with bridge {}.", bridgeUID.getAsString());

        var rooms = bridgeHandler.getRooms();

        if (rooms != null) {
            for (var room : rooms) {
                for (var device : room.devices) {
                    String label = "MyLight - ".concat(device.name.replaceAll("\\P{Alnum}", "_"));

                    @Nullable
                    ThingTypeUID thingTypeUID = switch (device.type_id) {
                        case "my_smart_battery":
                            yield THING_TYPE_MYLIGHT_SMART_BATTERY;
                        default:
                            yield null;
                    };

                    if (thingTypeUID != null) {
                        thingDiscovered(
                                DiscoveryResultBuilder.create(new ThingUID(thingTypeUID, bridgeUID, device.device_id))
                                        .withThingType(thingTypeUID) //
                                        .withProperty(PROPERTY_THING_DEVICE_ID, device.device_id) //
                                        .withRepresentationProperty(PROPERTY_THING_DEVICE_ID) //
                                        .withLabel(label).withBridge(bridgeUID).build());
                    }
                }
            }
        }
    }
}
