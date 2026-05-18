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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.util.ThingHandlerHelper;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.obones.binding.mylight.internal.config.MyLightBridgeConfiguration;
import com.obones.binding.mylight.internal.connection.MyLightConnection;
import com.obones.binding.mylight.internal.connection.MyLightHttpConnection;
import com.obones.binding.mylight.internal.connection.MyLightRoomsApiResponse;
import com.obones.binding.mylight.internal.connection.MyLightStatesApiResponse;
import com.obones.binding.mylight.internal.utils.Localization;

/**
 * <B>Common interaction with the </B><I>MyLight</I><B> bridge.</B>
 * <P>
 * It implements the communication between <B>OpenHAB</B> and the <I>MyLight</I> Bridge:
 * <UL>
 * <LI><B>OpenHAB</B> Event Bus &rarr; <I>MyLight</I> <B>bridge</B>
 * <P>
 * Sending commands and value updates.</LI>
 * </UL>
 * <UL>
 * <LI><I>MyLight</I> <B>bridge</B> &rarr; <B>OpenHAB</B>:
 * <P>
 * Retrieving information by sending a Refresh command.</LI>
 * </UL>
 * <P>
 * Entry point for this class is the method
 * {@link MyLightBridgeHandler#handleCommand handleCommand}.
 *
 * @author Olivier Sannier - Initial contribution.
 */
@NonNullByDefault
public class MyLightBridgeHandler extends BaseBridgeHandler {

    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightBridgeHandler.class);

    public Localization localization;
    public String authToken = "";

    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable MyLightConnection connection;
    private @Nullable MyLightStatesApiResponse states;
    private @Nullable MyLightRoomsApiResponse rooms;
    private HttpClient httpClient;

    private static final long INITIAL_DELAY_IN_SECONDS = 15;
    private static final long AUTH_TOKEN_REFRESH_DELAY_HOURS = 2;
    private static final Gson gson = new Gson();

    private ZonedDateTime lastAuthTokenDateTime;

    /*
     * ************************
     * ***** Constructors *****
     */

    public MyLightBridgeHandler(final Bridge bridge, Localization localization, HttpClient httpClient) {
        super(bridge);
        logger.trace("MyLightBridgeHandler(constructor with bridge={}, localization={}) called.", bridge, localization);
        this.localization = localization;
        this.httpClient = httpClient;
        logger.debug("Creating a MyLightBridgeHandler for thing '{}'.", getThing().getUID());

        lastAuthTokenDateTime = ZonedDateTime.now().minusDays(5); // a long way in the past to force initial update
    }

    // Provisioning/Deprovisioning methods *****

    @Override
    public void initialize() {
        // set the thing status to UNKNOWN temporarily and let the background task decide the real status
        updateStatus(ThingStatus.UNKNOWN);

        // take care of unusual situations...
        if (scheduler.isShutdown()) {
            logger.warn("initialize(): scheduler is shutdown, aborting initialization.");
            return;
        }

        MyLightBridgeConfiguration config = getConfigAs(MyLightBridgeConfiguration.class);

        boolean configIsValid = validateConfig(config);
        if (!configIsValid) {
            logger.warn("initialize(): config is invalid, aborting initialization");
            return;
        }

        connection = new MyLightHttpConnection(httpClient, config.baseURI);

        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob == null || localRefreshJob.isCancelled()) {
            logger.debug("Start refresh job at interval {} min.", config.refreshInterval);
            refreshJob = scheduler.scheduleWithFixedDelay(this::updateThings, INITIAL_DELAY_IN_SECONDS,
                    TimeUnit.MINUTES.toSeconds(config.refreshInterval), TimeUnit.SECONDS);
        }

        logger.trace("initialize(): initialize bridge configuration parameters.");
    }

    @Override
    public void dispose() {
        logger.debug("Dispose MyLight bridge handler '{}'.", getThing().getUID());
        ScheduledFuture<?> localRefreshJob = refreshJob;
        if (localRefreshJob != null && !localRefreshJob.isCancelled()) {
            logger.debug("Stop refresh job.");
            if (localRefreshJob.cancel(true)) {
                refreshJob = null;
            }
        }
    }

    /**
     * NOTE: It takes care by calling {@link #handleCommand} with the REFRESH command, that every used channel is
     * initialized.
     */
    @Override
    public void channelLinked(ChannelUID channelUID) {
        if (thing.getStatus() == ThingStatus.ONLINE) {
            logger.trace("channelLinked({}) refreshing channel value with help of handleCommand as Thing is online.",
                    channelUID.getAsString());
            handleCommand(channelUID, RefreshType.REFRESH);
        } else {
            logger.trace("channelLinked({}) doing nothing as Thing is not online.", channelUID.getAsString());
        }
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        logger.trace("channelUnlinked({}) called.", channelUID.getAsString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand({}): command {} on channel {} will be scheduled.", Thread.currentThread(), command,
                channelUID.getAsString());
        logger.debug("handleCommand({},{}) called.", channelUID.getAsString(), command);

        if (command instanceof RefreshType) {
            scheduler.schedule(this::updateThings, INITIAL_DELAY_IN_SECONDS, TimeUnit.SECONDS);
        } else {
            logger.debug("The MyLight binding is a read-only binding and cannot handle command '{}'.", command);
        }

        logger.trace("handleCommand({}) done.", Thread.currentThread());
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        scheduler.schedule(() -> {
            updateThing((MyLightBaseThingHandler) childHandler, childThing);
        }, INITIAL_DELAY_IN_SECONDS, TimeUnit.SECONDS);
    }

    private class LoginReply {
        // {"status":"ok","authToken":"1WNOSOtwp1tgYav9-xo22GZTCzyElhHLQ"}
        public String status = "";
        public String authToken = "";
    }

    private boolean ensureValidAuthToken() {
        if (lastAuthTokenDateTime.isBefore(ZonedDateTime.now().minusHours(AUTH_TOKEN_REFRESH_DELAY_HOURS))) {
            MyLightBridgeConfiguration config = getConfigAs(MyLightBridgeConfiguration.class);

            var loginResult = connection.login(config.email, config.password);
            if (loginResult.successful) {
                updateState(CHANNEL_BRIDGE_LAST_UPDATED, new DateTimeType(ZonedDateTime.now()));
                LoginReply reply = gson.fromJson(loginResult.serverReply, LoginReply.class);

                if (reply.status.equals("ok")) {
                    authToken = reply.authToken;
                    return true;
                }
            }

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, loginResult.serverReply);
            return false;
        }

        return true;
    }

    private void updateThings() {
        if (ensureValidAuthToken()) {
            updateState(CHANNEL_BRIDGE_LAST_UPDATED, new DateTimeType(ZonedDateTime.now()));
            updateStatus(ThingStatus.ONLINE);

            states = connection.getStates(authToken);
            rooms = connection.getRooms(authToken);

            List<Thing> children = getThing().getThings().stream().filter(Thing::isEnabled)
                    .collect(Collectors.toList());
            if (!children.isEmpty()) {
                for (Thing thing : children) {
                    updateThing((MyLightBaseThingHandler) thing.getHandler(), thing);
                }
            }
        }
    }

    public ThingStatus updateThing(@Nullable MyLightBaseThingHandler handler, Thing thing) {
        if (this.getThing().getStatus().equals(ThingStatus.ONLINE) && handler != null
                && ThingHandlerHelper.isHandlerInitialized(handler)) {

            var states = this.states;
            var rooms = this.rooms;
            if (states != null && rooms != null) {
                handler.updateDeviceProperties(rooms);
                handler.updateData(states);
                return thing.getStatus();
            }
        }

        logger.debug("Cannot update thing '{}'.", thing.getUID());
        return ThingStatus.OFFLINE;
    }

    private boolean validateConfig(MyLightBridgeConfiguration config) {
        return (!config.baseURI.trim().isEmpty()) && //
                (config.refreshInterval > 0) && //
                (!config.email.trim().isEmpty()) && //
                (!config.password.trim().isEmpty());
    }
}
