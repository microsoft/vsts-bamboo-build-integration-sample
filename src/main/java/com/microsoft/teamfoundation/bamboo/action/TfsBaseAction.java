package com.microsoft.teamfoundation.bamboo.action;

import com.atlassian.bamboo.build.BuildLoggerManager;
import com.atlassian.bamboo.security.EncryptionService;
import com.microsoft.teamfoundation.bamboo.BambooSecret;
import com.microsoft.teamfoundation.bamboo.Constants;
import com.microsoft.teamfoundation.plugin.TfsBuildFacadeFactory;
import com.microsoft.teamfoundation.plugin.TfsClientFactory;
import com.microsoft.teamfoundation.plugin.TfsSecret;
import com.microsoft.teamfoundation.plugin.impl.TfsBuildFacadeFactoryImpl;
import com.microsoft.teamfoundation.plugin.impl.TfsClient;
import com.microsoft.teamfoundation.plugin.impl.TfsClientFactoryImpl;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by yacao on 4/30/2015.
 */
public abstract class TfsBaseAction {

    private EncryptionService encryptionService;

    private TfsBuildFacadeFactory tfsBuildFacadeFactory;

    private TfsClientFactory tfsClientFactory;

    private BuildLoggerManager buildLoggerManager;

    protected TfsClient getTfsClient(Map<String, String> configurations) throws URISyntaxException{
        String serverUrl = configurations.get(Constants.TFS_SERVER_URL);
        String username = configurations.get(Constants.TFS_USERNAME);
        TfsSecret password = new BambooSecret(getEncryptionService(), configurations.get(Constants.TFS_PASSWORD));

        TfsClient tfsClient = getTfsClientFactory().getValidatedClient(serverUrl, username, password);

        if (tfsClient == null) {
            throw new RuntimeException("Could not connect to the specified URL with provided credentials.");
        }

        return tfsClient;
    }

    // this will be spring DI'ed
    public EncryptionService getEncryptionService() {
        return encryptionService;
    }

    public BuildLoggerManager getBuildLoggerManager() {
        return buildLoggerManager;
    }

    // for some reason spring was not able to inject those two classes on a remote agent
    // default to standard singleton getter
    public synchronized TfsBuildFacadeFactory getTfsBuildFacadeFactory() {
        if (this.tfsBuildFacadeFactory == null) {
            this.tfsBuildFacadeFactory = new TfsBuildFacadeFactoryImpl();
        }
        return tfsBuildFacadeFactory;
    }

    public synchronized TfsClientFactory getTfsClientFactory() {
        if(this.tfsClientFactory == null) {
            this.tfsClientFactory = new TfsClientFactoryImpl();
        }
        return tfsClientFactory;
    }

    /* spring setters */
    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setTfsBuildFacadeFactory(TfsBuildFacadeFactory tfsBuildFacadeFactory) {
        this.tfsBuildFacadeFactory = tfsBuildFacadeFactory;
    }

    public void setTfsClientFactory(TfsClientFactory tfsClientFactory) {
        this.tfsClientFactory = tfsClientFactory;
    }

    public void setBuildLoggerManager(final BuildLoggerManager buildLoggerManager) {
        this.buildLoggerManager = buildLoggerManager;
    }
}
