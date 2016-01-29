package com.microsoft.teamfoundation.bamboo.configuration;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.TopLevelPlan;
import com.atlassian.bamboo.security.EncryptionService;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.atlassian.bamboo.v2.build.BaseBuildConfigurationAwarePlugin;
import com.atlassian.bamboo.v2.build.configuration.MiscellaneousBuildConfigurationPlugin;
import com.atlassian.bamboo.ww2.actions.build.admin.create.BuildConfiguration;
import com.microsoft.teamfoundation.bamboo.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 *  This class records all the configuration settings required for posting the job record from
 *  Atlassian Bamboo to Microsoft Team Foundation Server / Visual Studio Online
 *
 *  @author Yang Cao
 */
public class TfsConfigurator extends BaseBuildConfigurationAwarePlugin
        implements MiscellaneousBuildConfigurationPlugin {

    private static final Logger log = Logger.getLogger(TfsConfigurator.class);

    private EncryptionService encryptionService;

    @Override
    public boolean isApplicableTo(@NotNull Plan plan) {
        return plan instanceof TopLevelPlan;
    }

    public void addDefaultValues(@NotNull BuildConfiguration buildConfiguration) {
    }

    public void prepareConfigObject(@NotNull BuildConfiguration buildConfiguration) {
        if (buildConfiguration.getBoolean(Constants.TFS_PLUGIN_ENABLED)) {
            if (buildConfiguration.getBoolean(Constants.TFS_PASSWORD_CHANGE)) {
                buildConfiguration.clearProperty(Constants.TFS_PASSWORD_CHANGE);

                String clearTextPassword = buildConfiguration.getString(Constants.TFS_PASSWORD_CLEARTEXT);
                buildConfiguration.clearProperty(Constants.TFS_PASSWORD_CLEARTEXT);

                if (StringUtils.isNotBlank(clearTextPassword)) {
                    buildConfiguration.setProperty(Constants.TFS_PASSWORD, encryptionService.encrypt(clearTextPassword));
                }
            }
        }
    }

    @NotNull
    public ErrorCollection validate(@NotNull BuildConfiguration buildConfiguration) {
        ErrorCollection errorCollection = super.validate(buildConfiguration);

        if (buildConfiguration.getBoolean(Constants.TFS_PLUGIN_ENABLED)) {
            checkValueExists(errorCollection, "Username", buildConfiguration.getString(Constants.TFS_USERNAME));
            checkValueExists(errorCollection, "Project", buildConfiguration.getString(Constants.TFS_PROJECT_NAME));
            checkValueExists(errorCollection, "Build Definition", buildConfiguration.getString(Constants.TFS_BUILD_DEFINITION));

            String serverUrl = buildConfiguration.getString(Constants.TFS_SERVER_URL);
            checkValueExists(errorCollection, "Server URL", serverUrl);
            if (StringUtils.isNotBlank(serverUrl)) {
                try {
                    URI uri = new URI(serverUrl);
                } catch (URISyntaxException e) {
                    errorCollection.addError("Server URL", "Server URL is not valid with error: "+e.getMessage());
                }
            }
        }

        return errorCollection;
    }

    private void checkValueExists(ErrorCollection errorCollection, String field, String value) {
        if (StringUtils.isBlank(value)) {
            errorCollection.addError(field, field+" could not be empty.");
        }
    }

    /* Spring setter */
    public void setEncryptionService(final EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }
}
