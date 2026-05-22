package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.credentials.CostAwareProviderConfiguration;

import java.util.Map;

public class CostAwareConfigurationReader {
    private final CostAwareConfiguration costAwareConfiguration;

    public CostAwareConfigurationReader(CostAwareConfiguration costAwareConfiguration) {
        this.costAwareConfiguration = costAwareConfiguration;
    }

    public Integer getVcpuCores() {
        return costAwareConfiguration.vcpuCores;
    }

    public Double getMemoryGiB() {
        return costAwareConfiguration.memoryGiB;
    }

    public Double getGpuMemoryGiB() {
        return costAwareConfiguration.gpuMemoryGiB;
    }

    public int getMinSpotInstances() {
        return costAwareConfiguration.minSpotInstances;
    }

    public int getMaxSpotInstances() {
        return costAwareConfiguration.maxSpotInstances;
    }

    public CostAwareProviderConfiguration getProviderConfiguration() {
        return costAwareConfiguration.providerConfiguration;
    }

    public String getDockerImage() {
        return costAwareConfiguration.dockerImage;
    }

    public String getImageArchitecture() {
        return costAwareConfiguration.imageArchitecture;
    }

    public boolean isUseCurrentEnvironmentVariables() {
        return costAwareConfiguration.useCurrentEnvironmentVariables;
    }

    public Map<String, String> getAdditionalEnvironmentVariables() {
        return costAwareConfiguration.additionalEnvironmentVariables;
    }

    public String[] getRegions() {
        return costAwareConfiguration.regions;
    }

    public boolean isEnabled() {
        return costAwareConfiguration.enabled;
    }
}
