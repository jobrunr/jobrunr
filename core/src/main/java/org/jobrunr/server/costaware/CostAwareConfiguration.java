package org.jobrunr.server.costaware;

import org.jobrunr.server.costaware.credentials.CostAwareAwsEC2ProviderConfiguration;
import org.jobrunr.server.costaware.credentials.CostAwareProviderConfiguration;

import java.util.HashMap;
import java.util.Map;

// TODO: is it also relevant for Karpenter / Google Cloud functions / AWS lambdas
public class CostAwareConfiguration {
    public static final Integer DEFAULT_VCPU_CORES = 1;
    public static final Double DEFAULT_MEMORY_GIB = 2.0;
    public static final Double DEFAULT_GPU_MEMORY_GIB = 0.0;
    public static final int DEFAULT_SPOT_INSTANCE_AMOUNT = 2;
    public static final String COST_AWARE_API_URL = "http://localhost:9000/cost-aware";

    Integer vcpuCores = DEFAULT_VCPU_CORES;
    Double memoryGiB = DEFAULT_MEMORY_GIB;
    Double gpuMemoryGiB = DEFAULT_GPU_MEMORY_GIB;
    int minSpotInstances = 0;
    int maxSpotInstances = DEFAULT_SPOT_INSTANCE_AMOUNT;

    CostAwareProviderConfiguration providerConfiguration;
    String dockerImage;
    String imageArchitecture = "x86_64";
    String[] regions = {"eu-north-1"};
    boolean useCurrentEnvironmentVariables = true;
    Map<String, String> additionalEnvironmentVariables = new HashMap<>();

    public CostAwareConfiguration(CostAwareProviderConfiguration providerConfiguration, String dockerImage) {
        this.providerConfiguration = providerConfiguration;
        this.dockerImage = dockerImage;
    }

    /**
     * Returns the default configuration for cost aware jobs, including a minimum of 1 vcpu core and 2 GiB of memory per instance and 2 spot instances.
     *
     * @param providerConfiguration the credentials necessary for connecting to the provider, must be one of: {@link CostAwareAwsEC2ProviderConfiguration}
     * @param dockerImage           the url of the docker image within your provider
     * @return the default CostAwareConfiguration
     */
    public static CostAwareConfiguration usingStandardCostAwareConfiguration(CostAwareProviderConfiguration providerConfiguration, String dockerImage) {
        return new CostAwareConfiguration(providerConfiguration, dockerImage);
    }

    /**
     * Allows to configure the minimum hardware requirements for your spot instances
     *
     * @param vcpuCores    the amount of vcpu cores, if null then 1
     * @param memoryGiB    the GiB's of memory, if null then 2
     * @param gpuMemoryGiB the GiB's of gpu memory, if null then 0
     * @return the same configuration instance
     */
    public CostAwareConfiguration andHardwareConfiguration(Integer vcpuCores, Double memoryGiB, Double gpuMemoryGiB) {
        this.vcpuCores = vcpuCores == null ? DEFAULT_VCPU_CORES : vcpuCores;
        this.memoryGiB = memoryGiB == null ? DEFAULT_MEMORY_GIB : memoryGiB;
        this.gpuMemoryGiB = gpuMemoryGiB == null ? DEFAULT_GPU_MEMORY_GIB : gpuMemoryGiB;
        return this;
    }

    /**
     * Allows to configure the amount of spot instances required
     *
     * @param minSpotInstances the minimum spot instance amount, must be 1 or higher
     * @param maxSpotInstances the maximum spot instance amount
     * @return the same configuration instance
     */
    public CostAwareConfiguration andSpotInstanceAmount(int minSpotInstances, int maxSpotInstances) {
        if (minSpotInstances <= 0 || maxSpotInstances < minSpotInstances) throw new IllegalArgumentException("You can't have 0 or less spot instances");
        this.maxSpotInstances = maxSpotInstances;
        this.minSpotInstances = minSpotInstances;
        return this;
    }

    /**
     * Allows to change the architecture of the supplier docker image
     *
     * @param imageArchitecture the image architecture
     * @return the same configuration instance
     */
    public CostAwareConfiguration andImageArchitecture(String imageArchitecture) {
        if (!imageArchitecture.equals("x86_64") && !imageArchitecture.equals("amd64"))
            throw new IllegalArgumentException("Image architecture must be either x86_64 or amd64");
        this.imageArchitecture = imageArchitecture;
        return this;
    }

    /**
     * Allows to stop the spot instances inheriting the environment variables from the master server
     *
     * @return the same configuration instance
     */
    public CostAwareConfiguration andNotUsingCurrentEnvironmentConfiguration() {
        this.useCurrentEnvironmentVariables = false;
        return this;
    }

    /**
     * Allows to add additional environment variables beyond your default configuration
     *
     * @param additionalEnvironmentVariables the additional environment variables, key must use the correct naming for the variable
     * @return the same configuration instance
     */
    public CostAwareConfiguration andUsingAdditionalEnvironmentVariables(Map<String, String> additionalEnvironmentVariables) {
        this.additionalEnvironmentVariables = additionalEnvironmentVariables;
        return this;
    }

    public CostAwareConfiguration andUsingRegions(String[] regions) {
        this.regions = regions;
        return this;
    }
}
