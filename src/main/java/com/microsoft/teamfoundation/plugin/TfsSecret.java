// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin;

/**
 * Created by yacao on 4/27/2015.
 */
public interface TfsSecret {

    /**
     * return the stored secret in clear text
     *
     * @return clear text secret
     */
    public String getSecret();
}
