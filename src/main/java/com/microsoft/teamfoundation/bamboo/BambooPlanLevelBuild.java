package com.microsoft.teamfoundation.bamboo;

import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.v2.build.BuildChanges;
import com.atlassian.bamboo.v2.build.BuildRepositoryChanges;
import com.microsoft.teamfoundation.build.webapi.model.BuildResult;
import com.microsoft.teamfoundation.plugin.ActualBuild;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

/**
 * Created by yacao on 4/27/2015.
 */
public class BambooPlanLevelBuild implements ActualBuild {

    private final Chain chain;
    private final ChainExecution chainExecution;

    public BambooPlanLevelBuild(final Chain chain, final ChainExecution chainExecution) {
        this.chain = chain;
        this.chainExecution = chainExecution;
    }

    @Override
    public String getDisplayName() {
        return chain.getBuildName();
    }

    @Override
    public BuildResult getBuildResult() {
        if (chainExecution.isSuccessful()) {
            return BuildResult.SUCCEEDED;
        }

        if (chainExecution.isStopping()) {
            return BuildResult.CANCELED;
        }

        return BuildResult.FAILED;
    }

    @Override
    public String getBuildSourceBranch() {
        //TODO Get real branch info if possible
        return "unknown";
    }

    @Override
    public String getBuildSourceCommit() {
        final BuildChanges buildChanges = this.chainExecution.getBuildChanges();
        if (buildChanges != null) {
            for (BuildRepositoryChanges changes : buildChanges.getRepositoryChanges()) {
                String revision = changes.getVcsRevisionKey();
                if (StringUtils.isNotBlank(revision)) {
                    return revision;
                }
            }
        }

        return "unknown";
    }

    @Override
    public Date getStartTime() {
        return chainExecution.getStartTime();
    }

    @Override
    public Date getFinishTime() {
        return new Date(getStartTime().getTime() + chainExecution.getElapsedTime());
    }

    @Override
    public String getWorkerName() {
        return "Bamboo";
    }
}
