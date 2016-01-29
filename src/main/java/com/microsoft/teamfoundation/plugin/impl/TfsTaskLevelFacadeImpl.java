// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin.impl;

import com.microsoft.teamfoundation.build.webapi.model.Build;
import com.microsoft.teamfoundation.plugin.TfsBuildFacade;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * This class can only interact with TFS on the task level
 *
 * To make it safe, this class overrides all job level functions on the TfsBuildFacade with no-ops
 */
public class TfsTaskLevelFacadeImpl extends TfsBuildFacadeImpl {

    private static final Logger logger = Logger.getLogger(TfsTaskLevelFacadeImpl.class.getName());

    public TfsTaskLevelFacadeImpl(final Build tfsBuild, final TfsClient tfsClient) {
        super(tfsBuild, null, tfsClient);
    }

    @Override
    public void startBuild() {
        logger.warning("Job level function callled in a task only context, please check your code!");
    }

    @Override
    public void finishBuild() {
        logger.warning("Job level function callled in a task only context, please check your code!");
    }

    @Override
    public void uploadJobLog(List<String> logLines) throws IOException  {
        logger.warning("Job level function callled in a task only context, please check your code!");
    }
}
