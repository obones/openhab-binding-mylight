/**
 * Copyright (c) 2023-2024 Olivier Sannier
 ** See the NOTICE file(s) distributed with this work for additional
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

import javax.ws.rs.core.UriBuilder;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.library.types.RawType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MyLightHttpConnection} represents an HTTP connection to the MyLight API and provides answers
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public class MyLightHttpConnection implements MyLightConnection {
    private @NonNullByDefault({}) final Logger logger = LoggerFactory.getLogger(MyLightHttpConnection.class);

    private String baseURI;

    
    public MyLightHttpConnection(String baseURI) {
        this.baseURI = baseURI;
    }

    private UriBuilder prepareUriBuilder(URI baseURI, String path) {
        UriBuilder builder = UriBuilder.fromUri(baseURI).path(path) //
                .queryParam("format", "flatbuffers") //
                .queryParam("timezone", "UTC");

        return builder;
    }

    private MyLightRoomsApiResponse getResponse(UriBuilder builder) {
        String url = builder.build().toString();

        logger.debug("Calling MyLight on {}", url);
        RawType data = HttpUtil.downloadData(url, null, false, -1);
        if (data == null) {
            logger.warn("Data was null");
            return new MyLightRoomsApiResponse();
        }

        return new MyLightRoomsApiResponse();
    }

    private @Nullable URI getUri() {
        try {
            return new URI(baseURI);
        } catch (URISyntaxException e) {
            logger.error("Invalid URI", e);
            return null;
        }
    }

    public MyLightRoomsApiResponse getRooms()
    {
        @Nullable
        URI uri = getUri();
        if (uri == null)
            return new MyLightRoomsApiResponse();

        UriBuilder builder = prepareUriBuilder(uri, "api");

        /*if (dailyDays != null) {
            builder.queryParam("forecast_days", dailyDays);
            builder.queryParam("daily", String.join(",", requiredDailyFields));
            if (pastDays != null) {
                builder.queryParam("past_days", pastDays);
            }
        }*/

        return getResponse(builder);
    }
}
