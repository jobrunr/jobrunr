package org.jobrunr.dashboard.ui.model;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.utils.JarUtils;
import org.jspecify.annotations.Nullable;

public class VersionUIModel {

    private String version;
    private boolean allowAnonymousDataUsage;
    private @Nullable String clusterId;
    private @Nullable String storageProviderType;

    private VersionUIModel() {
        this.version = JarUtils.getVersion(JobRunr.class);
        this.allowAnonymousDataUsage = false;
    }

    private VersionUIModel(String clusterId, String storageProviderType) {
        this.storageProviderType = storageProviderType;
        this.version = JarUtils.getVersion(JobRunr.class);
        this.allowAnonymousDataUsage = true;
        this.clusterId = clusterId;
    }

    public String getVersion() {
        return version;
    }

    public boolean isAllowAnonymousDataUsage() {
        return allowAnonymousDataUsage;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getStorageProviderType() {
        return storageProviderType;
    }

    public static VersionUIModel withAnonymousDataUsage(String clusterId, String storageProviderType) {
        return new VersionUIModel(clusterId, storageProviderType);
    }

    public static VersionUIModel withoutAnonymousDataUsage() {
        return new VersionUIModel();
    }
}
