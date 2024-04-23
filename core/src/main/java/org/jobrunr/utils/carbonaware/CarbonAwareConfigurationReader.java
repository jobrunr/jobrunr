package org.jobrunr.utils.carbonaware;

import java.time.Duration;

/**
 * Internal class for JobRunr to access all {@link CarbonAwareConfiguration} details
 */
public class CarbonAwareConfigurationReader {
    private final CarbonAwareConfiguration carbonAwareConfiguration;

    public CarbonAwareConfigurationReader(CarbonAwareConfiguration carbonAwareConfiguration) {
        this.carbonAwareConfiguration = carbonAwareConfiguration;
    }

    public static String getCarbonAwareApiUrl(String hostName) {
        return hostName + getCarbonAwareApiUrlPath();
    }

    public static String getCarbonAwareApiUrlPath() {
        return "/carbon-intensity/day-ahead-energy-prices?version=1";
    }

    public String getAreaCode() {
        return carbonAwareConfiguration.areaCode;
    }

    public String getState() {
        return carbonAwareConfiguration.state;
    }

    public String getCloudProvider() {
        return carbonAwareConfiguration.cloudProvider;
    }

    public String getCloudRegion() {
        return carbonAwareConfiguration.cloudRegion;
    }

    public String getCarbonAwareApiUrl() {
        return carbonAwareConfiguration.carbonAwareApiUrl;
    }

    public Duration getApiClientConnectTimeout() {
        return carbonAwareConfiguration.apiClientConnectTimeout;
    }

    public Duration getApiClientReadTimeout() {
        return carbonAwareConfiguration.apiClientReadTimeout;
    }
}
