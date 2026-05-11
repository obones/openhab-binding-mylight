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
package com.obones.binding.mylight.internal.connection;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link MyLightHttpConnection} represents an HTTP connection to the MyLight API and provides answers
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public class MyLightHttpConnection implements MyLightConnection {
    private static final int TIMEOUT_MS = 5000;
    private static final Gson gson = new Gson();

    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightHttpConnection.class);

    private String baseURI;
    private HttpClient httpClient;

    public MyLightHttpConnection(HttpClient httpClient, String baseURI) {
        this.baseURI = baseURI;
        this.httpClient = httpClient;
    }

    private UriBuilder prepareUriBuilder(URI baseURI, @Nullable String authToken, String path) {
        UriBuilder builder = UriBuilder.fromUri(baseURI).path(path);

        if (authToken != null)
            builder.queryParam("authToken", authToken);

        return builder;
    }

    private UriBuilder prepareUriBuilder(URI baseURI, String path) {
        return prepareUriBuilder(baseURI, null, path);
    }

    private ContentResponse getResponse(UriBuilder builder)
            throws InterruptedException, TimeoutException, ExecutionException {
        String url = builder.build().toString();

        Request request = httpClient.newRequest(url).method(HttpMethod.GET).timeout(TIMEOUT_MS, TimeUnit.MILLISECONDS);

        logger.trace("Calling url: {}", request.getURI());

        ContentResponse response = request.send();

        return response;
    }

    private @Nullable URI getUri() {
        try {
            return new URI(baseURI);
        } catch (URISyntaxException e) {
            logger.error("Invalid URI", e);
            return null;
        }
    }

    public LoginResult login(String email, String password) {
        @Nullable
        URI uri = getUri();
        if (uri == null)
            return new LoginResult(false, "URI is invalid");

        UriBuilder builder = prepareUriBuilder(uri, "auth") //
                .queryParam("email", email) //
                .queryParam("password", password);

        try {
            ContentResponse response = getResponse(builder);
            int status = response.getStatus();

            switch (status) {
                case HttpStatus.FORBIDDEN_403:
                    return new LoginResult(false, "Login forbidden: " + response.getContentAsString());
                case HttpStatus.OK_200:
                    return new LoginResult(true, response.getContentAsString());
                default:
                    return new LoginResult(false, "Unexpected reply upon login: " + response.getContentAsString());
            }
        } catch (Exception e) {
            return new LoginResult(false, e.toString());
        }
    }

    private @Nullable JsonObject getApiResult(String authToken, String path) {

        URI uri = getUri();
        if (uri == null)
            return null;

        UriBuilder builder = prepareUriBuilder(uri, authToken, path);

        try {
            ContentResponse response = getResponse(builder);
            if (response.getStatus() == HttpStatus.OK_200) {
                var contentAsString = response.getContentAsString();

                var root = JsonParser.parseString(contentAsString).getAsJsonObject();
                var replyState = root.get("status").getAsString();
                if (replyState.equals("ok"))
                    return root;
                else
                    logger.error("Failure in response : {}", contentAsString);
            }
        } catch (Exception e) {
            logger.error("Unable to get {}: {}", path, e.toString());
        }

        return null;
    }

    public MyLightRoomsApiResponse getRooms(String authToken) {
        var root = getApiResult(authToken, "rooms");
        if (root != null) {
            var roomsResponse = gson.fromJson(root.get("rooms").getAsJsonArray(), MyLightRoomsApiResponse.class);

            if (roomsResponse == null)
                logger.error("Returned response for rooms is not valid: {}", gson.toJson(root));
            else
                return roomsResponse;
        }

        return new MyLightRoomsApiResponse();
    }

    public MyLightStatesApiResponse getStates(String authToken) {
        var root = getApiResult(authToken, "states");
        if (root != null) {
            var statesResponse = gson.fromJson(root.get("deviceStates").getAsJsonArray(),
                    MyLightStatesApiResponse.class);

            if (statesResponse == null)
                logger.error("Returned response for states is not valid: {}", gson.toJson(root));
            else
                return statesResponse;
        }

        return new MyLightStatesApiResponse();
    }
}
