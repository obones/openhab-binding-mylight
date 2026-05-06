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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MyLightConnection} represents a connection to the MyLight API and provides answers
 *
 * @author Olivier Sannier - Initial contribution
 */
@NonNullByDefault
public interface MyLightConnection {
    MyLightRoomsApiResponse getRooms();
}
