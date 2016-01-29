// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin;

import com.microsoft.teamfoundation.plugin.impl.TfsClient;
import com.microsoft.vss.client.core.model.VssServiceException;

import java.net.URISyntaxException;

public interface TfsClientFactory {

    /**
     * Create a verified REST client for TFS
     *
     * If a valid client can not be constructed, will throw exception
     *
     * @param url TFS collection level url
     * @param username
     * @param password
     * @return new REST TFS client
     * @throws URISyntaxException
     */
    TfsClient getValidatedClient(String url, String username, TfsSecret password) throws URISyntaxException, VssServiceException;
}
