// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin;

import com.microsoft.teamfoundation.build.webapi.model.DefinitionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;

/**
 * Create a facade that can be used to update TFS builds
 */
public interface TfsBuildFacadeFactory {

    /**
     * Create (queue) a build on TFS side that acts like a container for this jenkins build
     *
     * @param projectId id or name of TFS/VSO project
     * @param buildDefinition id of the build definition
     * @param actualBuild
     * @param tfsClient
     * @return a TfsBuildInstance
     */
    TfsBuildFacade createBuildOnTfs(final String projectId, int buildDefinition,
                                    final ActualBuild actualBuild, final TfsClient tfsClient);


    /**
     * Create (queue) a build on TFS side that acts like a container for this jenkins build
     *
     * @param projectReference TFS/VSO team project reference
     * @param buildDefinition  build definition reference
     * @param actualBuild
     * @param tfsClient
     * @return a TfsBuildInstance
     */
    TfsBuildFacade createBuildOnTfs(final TeamProjectReference projectReference, final DefinitionReference buildDefinition,
                                    final ActualBuild actualBuild, final TfsClient tfsClient);

    /**
     * Get a TfsBuildFacade when a build has been queued already on TFS side
     * @param tfsBuildId
     * @param actualBuild
     * @param tfsClient
     */
    TfsBuildFacade getBuildOnTfs(final int tfsBuildId, final ActualBuild actualBuild, final TfsClient tfsClient);

    /**
     * Get a TfsBuildFacade that can only interact with TFS on the tasks level. All job level functions will be no-ops
     *
     * It's assumed the build has been queued already on TFS side
     *
     * @param tfsBuildId
     * @param tfsClient
     * @return TfsBuildFacade
     */
    TfsBuildFacade getTaskLevelFacadeOnTfs(final int tfsBuildId, final TfsClient tfsClient);
}
