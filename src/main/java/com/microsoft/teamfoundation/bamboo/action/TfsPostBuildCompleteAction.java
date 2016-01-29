package com.microsoft.teamfoundation.bamboo.action;

import com.atlassian.bamboo.build.CustomPostBuildCompletedAction;
import com.atlassian.bamboo.build.logger.BuildLogUtils;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.chains.StageExecution;
import com.atlassian.bamboo.v2.build.BuildContext;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.microsoft.teamfoundation.distributedtask.webapi.model.TaskResult;
import com.microsoft.teamfoundation.bamboo.Constants;
import com.microsoft.teamfoundation.plugin.TfsBuildFacade;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;

/**
 * Created by yacao on 4/30/2015.
 */
public class TfsPostBuildCompleteAction extends TfsBaseAction implements CustomPostBuildCompletedAction {

    private BuildContext buildContext;

    @Override
    public void init(BuildContext buildContext) {
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

        final Map<String, String> customBuildData = this.buildContext.getCurrentResult().getCustomBuildData();
        String tfsBuildIdStr = customBuildData.get(Constants.TFS_BUILD_ID);
        String tfsTaskIdStr = customBuildData.get(Constants.TFS_TASK_ID);
        TfsClient tfsClient = getTfsClient(planLevelConfigs);

        if (StringUtils.isNotBlank(tfsBuildIdStr) && StringUtils.isNotBlank(tfsTaskIdStr)) {
            int tfsBuildId = Integer.parseInt(tfsBuildIdStr);
            UUID tfsTaskId = UUID.fromString(tfsTaskIdStr);

            TfsBuildFacade taskLevelFacadeOnTfs = getTfsBuildFacadeFactory().getTaskLevelFacadeOnTfs(tfsBuildId, tfsClient);

            taskLevelFacadeOnTfs.finishTaskRecord(tfsTaskId, new Date(), getBuildResult());
            uploadLogs(taskLevelFacadeOnTfs, tfsTaskId);
        }

        return buildContext;
    }

    private TaskResult getBuildResult() {
        BuildState buildState = buildContext.getCurrentResult().getBuildState();
        switch (buildState) {
            case SUCCESS:
                return TaskResult.SUCCEEDED;
            case FAILED:
                return TaskResult.FAILED;
            case UNKNOWN:
            default:
                //assume failure
                return TaskResult.FAILED;
        }
    }

    private void uploadLogs(TfsBuildFacade tfsBuildFacade, UUID tfsTaskId) {
        String logfile = BuildLogUtils.getLogFileName(buildContext.getResultKey());
        File directory = BuildLogUtils.getLogFileDirectory(buildContext.getEntityKey());
        try {
            FileInputStream fis = new FileInputStream(new File(directory, logfile));
            tfsBuildFacade.uploadTaskLog(tfsTaskId, fis);
            fis.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
