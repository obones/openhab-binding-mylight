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
package com.obones.binding.mylight.internal.factory;

import static com.obones.binding.mylight.internal.MyLightBindingConstants.*;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TimeZoneProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.obones.binding.mylight.internal.discovery.MyLightDiscoveryService;
import com.obones.binding.mylight.internal.handler.MyLightBridgeHandler;
import com.obones.binding.mylight.internal.handler.MyLightPowerCounterThingHandler;
import com.obones.binding.mylight.internal.handler.MyLightSmartBatteryThingHandler;
import com.obones.binding.mylight.internal.handler.MyLightVirtualCountersThingHandler;
import com.obones.binding.mylight.internal.handler.MyLightWaterHeaterThingHandler;
import com.obones.binding.mylight.internal.utils.Localization;

/**
 * The {@link MyLightHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, name = "binding.mylight")
public class MyLightHandlerFactory extends BaseThingHandlerFactory {
    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(ThingHandlerFactory.class);

    private Map<MyLightBridgeHandler, MyLightDiscoveryServiceAndRegistration> serviceAndRegistrations = new HashMap<>();

    private @NonNullByDefault({}) LocaleProvider localeProvider;
    private @NonNullByDefault({}) TranslationProvider i18nProvider;
    private TimeZoneProvider timeZoneProvider;
    private Localization localization = Localization.UNKNOWN;
    private final HttpClient httpClient;

    // Private

    private void updateLocalization() {
        if (Localization.UNKNOWN.equals(localization) && (localeProvider != null) && (i18nProvider != null)) {
            logger.trace("updateLocalization(): creating Localization based on locale={},translation={}).",
                    localeProvider, i18nProvider);
            localization = new Localization(localeProvider, i18nProvider);
        }
    }

    private void registerDeviceDiscoveryService(MyLightBridgeHandler bridgeHandler) {
        logger.trace("registerDeviceDiscoveryService({}) called.", bridgeHandler);

        serviceAndRegistrations.remove(bridgeHandler);

        MyLightDiscoveryService discoveryService = new MyLightDiscoveryService(bridgeHandler, localeProvider,
                i18nProvider);
        ServiceRegistration<?> discoveryServiceRegistration = bundleContext
                .registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());

        serviceAndRegistrations.put(bridgeHandler,
                new MyLightDiscoveryServiceAndRegistration(discoveryService, discoveryServiceRegistration));
    }

    private synchronized void unregisterDeviceDiscoveryService(MyLightBridgeHandler bridgeHandler) {
        logger.trace("unregisterDeviceDiscoveryService({}) called.", bridgeHandler);

        serviceAndRegistrations.remove(bridgeHandler);
    }

    private @Nullable ThingHandler createBridgeHandler(Thing thing, HttpClient httpClient) {
        logger.trace("createBridgeHandler({}) called for thing named '{}'.", thing.getUID(), thing.getLabel());
        MyLightBridgeHandler myLightBridgeHandler = new MyLightBridgeHandler((Bridge) thing, localization, httpClient);
        registerDeviceDiscoveryService(myLightBridgeHandler);
        return myLightBridgeHandler;
    }

    private @Nullable ThingHandler createSmartBatteryThingHandler(Thing thing) {
        logger.trace("createSmartBatteryThingHandler({}) called for thing named '{}'.", thing.getUID(),
                thing.getLabel());
        return new MyLightSmartBatteryThingHandler(thing, localization, timeZoneProvider);
    }

    private @Nullable ThingHandler createVirtualCountersThingHandler(Thing thing) {
        logger.trace("createVirtualCountersThingHandler({}) called for thing named '{}'.", thing.getUID(),
                thing.getLabel());
        return new MyLightVirtualCountersThingHandler(thing, localization, timeZoneProvider);
    }

    private @Nullable ThingHandler createPowerCounterThingHandler(Thing thing) {
        logger.trace("createPowerCounterThingHandler({}) called for thing named '{}'.", thing.getUID(),
                thing.getLabel());
        return new MyLightPowerCounterThingHandler(thing, localization, timeZoneProvider);
    }

    private @Nullable ThingHandler createWaterHeaterThingHandler(Thing thing) {
        logger.trace("createWaterHeaterThingHandler({}) called for thing named '{}'.", thing.getUID(),
                thing.getLabel());
        return new MyLightWaterHeaterThingHandler(thing, localization, timeZoneProvider);
    }

    // Constructor

    @Activate
    public MyLightHandlerFactory(final @Reference LocaleProvider givenLocaleProvider,
            final @Reference TranslationProvider givenI18nProvider,
            final @Reference TimeZoneProvider givenTimeZoneProvider,
            final @Reference HttpClientFactory httpClientFactory) {
        logger.trace("MyLightHandlerFactory(locale={},translation={}) called.", givenLocaleProvider, givenI18nProvider);
        localeProvider = givenLocaleProvider;
        i18nProvider = givenI18nProvider;
        timeZoneProvider = givenTimeZoneProvider;
        this.httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Reference
    protected void setLocaleProvider(final LocaleProvider givenLocaleProvider) {
        logger.trace("setLocaleProvider(): provided locale={}.", givenLocaleProvider);
        localeProvider = givenLocaleProvider;
        updateLocalization();
    }

    @Reference
    protected void setTranslationProvider(TranslationProvider givenI18nProvider) {
        logger.trace("setTranslationProvider(): provided translation={}.", givenI18nProvider);
        i18nProvider = givenI18nProvider;
        updateLocalization();
    }

    // Utility methods

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        boolean result = SUPPORTED_THINGS_BRIDGE.contains(thingTypeUID)
                || SUPPORTED_THINGS_ITEMS.contains(thingTypeUID);
        logger.trace("supportsThingType({}) called and returns {}.", thingTypeUID, result);
        return result;
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingHandler resultHandler = null;
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        // Handle Binding creation
        // Handle Bridge creation
        if (SUPPORTED_THINGS_BRIDGE.contains(thingTypeUID)) {
            resultHandler = createBridgeHandler(thing, httpClient);
        }
        // Handle creation of Things behind the Bridge
        else if (THING_TYPE_MYLIGHT_SMART_BATTERY.equals(thingTypeUID)) {
            resultHandler = createSmartBatteryThingHandler(thing);
        } else if (THING_TYPE_MYLIGHT_VIRTUAL_COUNTERS.equals(thingTypeUID)) {
            resultHandler = createVirtualCountersThingHandler(thing);
        } else if (THING_TYPE_MYLIGHT_POWER_COUNTER.equals(thingTypeUID)) {
            resultHandler = createPowerCounterThingHandler(thing);
        } else if (THING_TYPE_MYLIGHT_WATER_HEATER.equals(thingTypeUID)) {
            resultHandler = createWaterHeaterThingHandler(thing);
        } else {
            logger.warn("createHandler({}) failed: ThingHandler not found for {}.", thingTypeUID, thing.getLabel());
        }

        return resultHandler;
    }

    @Override
    protected void removeHandler(ThingHandler thingHandler) {
        // Handle Bridge removal
        if (thingHandler instanceof MyLightBridgeHandler) {
            logger.trace("removeHandler() removing bridge '{}'.", thingHandler.toString());
            unregisterDeviceDiscoveryService((MyLightBridgeHandler) thingHandler);
        }

        super.removeHandler(thingHandler);
    }
}
