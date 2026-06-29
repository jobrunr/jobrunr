package org.jobrunr.server.costaware.credentials;

import org.jobrunr.utils.CollectionUtils;

import java.util.Map;

// TODO should the package be provider?
public class CostAwareAwsEC2ProviderConfiguration implements CostAwareProviderConfiguration {
    final String accessKeyId;
    final String secretAccessKey;
    final String accountRegion;
    final String registryReaderProfile;

    public CostAwareAwsEC2ProviderConfiguration(String accessKeyId, String secretAccessKey, String accountRegion, String registryReaderProfile) {
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.accountRegion = accountRegion;
        this.registryReaderProfile = registryReaderProfile;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getAccountRegion() {
        return accountRegion;
    }

    public String getRegistryReaderProfile() {
        return registryReaderProfile;
    }

    @Override
    public Map<String, String> asMap() {
        return CollectionUtils.mapOf(
                "type", "AWS",
                "accessKeyId", accessKeyId,
                "secretAccessKey", secretAccessKey,
                "accountRegion", accountRegion,
                "registryReaderProfile", registryReaderProfile
        );
    }

    @Override
    public String getProvider() {
        return "aws";
    }
}
