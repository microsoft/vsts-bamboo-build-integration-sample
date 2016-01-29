// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.teamfoundation.plugin.impl;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.microsoft.teamfoundation.build.webapi.model.*;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.plugin.ActualBuild;
import com.microsoft.teamfoundation.plugin.TfsBuildFacade;
import com.microsoft.teamfoundation.plugin.TfsBuildFacadeFactory;

public class TfsBuildFacadeFactoryImpl implements TfsBuildFacadeFactory {

    private static final Logger logger = Logger.getLogger(TfsBuildFacadeFactoryImpl.class.getName());

    @Override
    public TfsBuildFacade createBuildOnTfs(String projectId, int buildDefinition, ActualBuild actualBuild, TfsClient tfsClient) {
        if (actualBuild == null || tfsClient == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        TeamProjectReference projectReference = tfsClient.getProjectClient().getProject(projectId);
        if (projectReference == null) {
            throw new RuntimeException(String.format("Could not find the project: %s", projectId));
        }

        DefinitionReference definitionReference = tfsClient.getBuildClient().getDefinition(projectReference.getId(), buildDefinition, null, null);
        if (definitionReference == null) {
            throw new RuntimeException(String.format("Could not find the buildDefinition: %d", buildDefinition));
        }

        return createBuildOnTfs(projectReference, definitionReference, actualBuild, tfsClient);
    }

    @Override
    public TfsBuildFacade createBuildOnTfs(TeamProjectReference projectReference, DefinitionReference definitionReference,
                                           ActualBuild actualBuild, TfsClient tfsClient) {
        if (actualBuild == null || tfsClient == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }

        List<AgentPoolQueue> queues = tfsClient.getBuildClient().getQueues(null);
        if (queues == null || queues.isEmpty()) {
            logger.info("Creating JenkinsPluginQueue on TeamFoundationServer");
            queues = createTfsBuildQueue(tfsClient);
        }

        AgentPoolQueue anyQueue = queues.get(0);
        Build buildContainer = createBuildContainer(projectReference, definitionReference, anyQueue, actualBuild);
        Build queuedBuild = tfsClient.getBuildClient().queueBuild(buildContainer, true);

        logger.info(String.format("Queued build on TFS with plan Id %s", queuedBuild.getOrchestrationPlan().getPlanId()));

        return new TfsBuildFacadeImpl(queuedBuild, actualBuild, tfsClient);
    }

    @Override
    public TfsBuildFacade getBuildOnTfs(int tfsBuildId, ActualBuild actualBuild, TfsClient tfsClient) {
        Build tfsBuild = tfsClient.getBuildClient().getBuild(tfsBuildId, null);

        return new TfsBuildFacadeImpl(tfsBuild, actualBuild, tfsClient);
    }

    @Override
    public TfsBuildFacade getTaskLevelFacadeOnTfs(int tfsBuildId, TfsClient tfsClient) {
        Build tfsBuild = tfsClient.getBuildClient().getBuild(tfsBuildId, null);

        return new TfsTaskLevelFacadeImpl(tfsBuild, tfsClient);
    }

    private List<AgentPoolQueue> createTfsBuildQueue(TfsClient tfsClient) {
        AgentPoolQueue queue = new AgentPoolQueue();
        queue.setName("pluginsQueue");

        queue = tfsClient.getBuildClient().createQueue(queue);

        return Collections.singletonList(queue);
    }

    private Build createBuildContainer(TeamProjectReference project, DefinitionReference definition, AgentPoolQueue queue, ActualBuild actualBuild) {
        Build b = new Build();
        b.setQueue(queue);
        b.setDefinition(definition);
        b.setProject(project);

        b.setParameters(String.format("{\"build.config\":\"%s\"}", actualBuild.getDisplayName()));
        b.setDemands(Collections.<Demand>emptyList());
        b.setQueueOptions(QueueOptions.DO_NOT_RUN);

        b.setSourceBranch(actualBuild.getBuildSourceBranch());

        return b;
    }
}
