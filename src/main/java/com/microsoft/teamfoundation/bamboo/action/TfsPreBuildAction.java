package com.microsoft.teamfoundation.bamboo.action;

import com.atlassian.bamboo.build.BuildExecutionManager;
import com.atlassian.bamboo.build.CustomPreBuildAction;
import com.atlassian.bamboo.buildqueue.manager.AgentManager;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.utils.error.SimpleErrorCollection;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.microsoft.teamfoundation.bamboo.Constants;
import com.microsoft.teamfoundation.plugin.TfsBuildFacade;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Created by yacao on 4/28/2015.
 */
public class TfsPreBuildAction extends TfsBaseAction implements CustomPreBuildAction {

    private BuildContext buildContext;
    private BuildExecutionManager buildExecutionManager;
    private AgentManager agentManager;

    @NotNull
    @Override
    public ErrorCollection validate(BuildConfiguration buildConfiguration) {
        return new SimpleErrorCollection();
    }

    @Override
    public void init(final BuildContext buildContext) {
        this.buildContext = buildContext;
    }

    @NotNull
    @Override
    public BuildContext call() throws InterruptedException, Exception {
        BuildContext parentBuildContext = this.buildContext.getParentBuildContext();
        if (parentBuildContext == null) {
            return buildContext;
        }

        Map<String, String> planLevelConfigs = parentBuildContext.getBuildDefinition().getCustomConfiguration();
        // return immediately if the plugin is not enabled
        if (!Boolean.parseBoolean(planLevelConfigs.get(Constants.TFS_PLUGIN_ENABLED))) {
            return buildContext;
        }

        TfsClient tfsClient = getTfsClient(planLevelConfigs);

        final Map<String, String> customBuildData = this.buildContext.getCurrentResult().getCustomBuildData();
        String tfsBuildIdStr = customBuildData.get(Constants.TFS_BUILD_ID);

        if (StringUtils.isNotBlank(tfsBuildIdStr)) {
            int tfsBuildId = Integer.parseInt(tfsBuildIdStr);

            TfsBuildFacade taskLevelFacadeOnTfs = getTfsBuildFacadeFactory().getTaskLevelFacadeOnTfs(tfsBuildId, tfsClient);

            UUID taskId = taskLevelFacadeOnTfs.createTaskRecord(buildContext.getShortName());
            customBuildData.put(Constants.TFS_TASK_ID, taskId.toString());

            //TODO Maybe get the real start time?
            taskLevelFacadeOnTfs.startTaskRecord(taskId, new Date(), getExecutingAgentName());
        }

        return buildContext;
    }

    private String getExecutingAgentName() {
        //TOOD get the real agent name
        return "yacao-pc";
    }

    public void setBuildExecutionManager(BuildExecutionManager buildExecutionManager) {
        this.buildExecutionManager = buildExecutionManager;
    }

    public void setAgentManager(AgentManager agentManager) {
        this.agentManager = agentManager;
    }
}
