package com.microsoft.teamfoundation.bamboo.action;

import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.security.EncryptionService;
import com.microsoft.teamfoundation.bamboo.BambooPlanLevelBuild;
import com.microsoft.teamfoundation.bamboo.BambooSecret;
import com.microsoft.teamfoundation.bamboo.Constants;
import com.microsoft.teamfoundation.plugin.*;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;
import org.apache.log4j.Logger;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by yacao on 4/27/2015.
 */
public class TfsPostChainAction extends TfsBaseAction implements PostChainAction {

    private static final Logger log = Logger.getLogger(TfsPostChainAction.class);

    @Override
    public void execute(final Chain chain, final ChainResultsSummary chainResultsSummary, final ChainExecution chainExecution) throws InterruptedException, Exception {
        Map<String, String> configurations = chain.getBuildDefinition().getCustomConfiguration();

        // return immediately if the plugin is not enabled
        if (!Boolean.parseBoolean(configurations.get(Constants.TFS_PLUGIN_ENABLED))) {
            return;
        }

        Integer tfsBuildId =  chainExecution.getExecutionContext().getValue(TfsPreChainAction.TFS_CONTEXT_KEY);
        if (tfsBuildId == null) {
            log.warn("Plugin is enabled but no TFS/VSO build container was found/created for this build.");
            return;
        }

        TfsClient tfsClient = getTfsClient(configurations);

        TfsBuildFacade tfsBuildFacade = getTfsBuildFacadeFactory().getBuildOnTfs(tfsBuildId, new BambooPlanLevelBuild(chain, chainExecution), tfsClient);

        tfsBuildFacade.finishBuild();
    }
}
