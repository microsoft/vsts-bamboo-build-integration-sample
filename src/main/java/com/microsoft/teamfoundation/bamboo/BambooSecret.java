package com.microsoft.teamfoundation.bamboo;

import com.atlassian.bamboo.security.EncryptionService;
import com.microsoft.teamfoundation.plugin.TfsSecret;

/**
 * Created by yacao on 4/27/2015.
 */
public class BambooSecret implements TfsSecret {

    private String encryptedSecret;
    private EncryptionService encryptionService;

    public BambooSecret(EncryptionService encryptionService, String encryptedSecret) {
        this.encryptionService = encryptionService;
        this.encryptedSecret = encryptedSecret;
    }

    @Override
    public String getSecret() {
        return encryptionService.decrypt(encryptedSecret);
    }
}
