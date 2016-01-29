package com.microsoft.teamfoundation.bamboo.action;

import com.atlassian.bamboo.chains.BuildExecution;
import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.StageExecution;
import com.atlassian.bamboo.chains.plugins.PreChainAction;
import com.atlassian.bamboo.utils.map.Key;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.microsoft.teamfoundation.bamboo.BambooPlanLevelBuild;
import com.microsoft.teamfoundation.bamboo.Constants;
import com.microsoft.teamfoundation.build.webapi.model.DefinitionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.plugin.*;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

/**
 * Created by yacao on 4/27/2015.
 */
public class TfsPreChainAction extends TfsBaseAction implements PreChainAction {

    public static final Key<Integer> TFS_CONTEXT_KEY = Key.createKey(Integer.class);

    @Override
    public void execute(final Chain chain, final ChainExecution chainExecution) throws InterruptedException, URISyntaxException {
        Map<String, String> configurations = chain.getBuildDefinition().getCustomConfiguration();

        // return immediately if the plugin is not enabled
        if (!Boolean.parseBoolean(configurations.get(Constants.TFS_PLUGIN_ENABLED))) {
            return;
        }

        TfsClient tfsClient = getTfsClient(configurations);

        String projectName = configurations.get(Constants.TFS_PROJECT_NAME);
        TeamProjectReference projectReference = tfsClient.getProjectClient().getProject(projectName);
        if (projectReference == null) {
            throw new RuntimeException("Failed to locate project " + projectName);
        }

        String buildDefinitionName = configurations.get(Constants.TFS_BUILD_DEFINITION);
        DefinitionReference definitionReference = getBuildDefinitionReference(tfsClient, projectReference, buildDefinitionName);
        if (definitionReference == null) {
            throw new RuntimeException("Failed to locate build definition " + buildDefinitionName);
        }

        TfsBuildFacade tfsBuildFacade = getTfsBuildFacadeFactory().createBuildOnTfs(projectReference, definitionReference,
                new BambooPlanLevelBuild(chain, chainExecution), tfsClient);

        tfsBuildFacade.startBuild();

        chainExecution.getExecutionContext().putIfAbsent(TFS_CONTEXT_KEY, tfsBuildFacade.getTfsBuildId());

        // now I have to populate the build number to all jobs within my plan
        populateAllJobsWithTfsBuildId(chainExecution, tfsBuildFacade);
    }

    private void populateAllJobsWithTfsBuildId(ChainExecution chainExecution, TfsBuildFacade tfsBuildFacade) {
        for (StageExecution stageExecution : chainExecution.getStages()) {
            for (BuildExecution buildExecution : stageExecution.getBuilds()) {
                BuildContext buildContext = buildExecution.getBuildContext();
                buildContext.getCurrentResult().getCustomBuildData().put(Constants.TFS_BUILD_ID, String.valueOf(tfsBuildFacade.getTfsBuildId()));
                buildContext.getCurrentResult().getCustomBuildData().put("StageName", buildExecution.getStageExecution().getName());
            }
        }
    }

    private DefinitionReference getBuildDefinitionReference(TfsClient tfsClient, TeamProjectReference projectReference, String buildDefinitionName) {
        List<DefinitionReference> references = tfsClient.getBuildClient().getDefinitions(projectReference.getId());
        for (DefinitionReference ref : references) {
            if (ref.getName().equals(buildDefinitionName)) {
                return ref;
            }
        }

        return null;
    }


}
