package org.jobrunr.utils.carbonaware;

import java.time.Duration;

import static org.jobrunr.utils.carbonaware.CarbonAwareConfigurationReader.getCarbonAwareApiUrl;

public class CarbonAwareConfiguration {

    public static String DEFAULT_CARBON_AWARE_API_URL = getCarbonAwareApiUrl("https://api.jobrunr.io");
    public static Duration DEFAULT_CLIENT_API_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static Duration DEFAULT_CLIENT_API_READ_TIMEOUT = Duration.ofSeconds(3);

     String carbonAwareApiUrl = DEFAULT_CARBON_AWARE_API_URL;
     String area;
     String state;
     String cloudProvider;
     String cloudRegion;
     Duration apiClientConnectTimeout = DEFAULT_CLIENT_API_CONNECT_TIMEOUT;
     Duration apiClientReadTimeout = DEFAULT_CLIENT_API_READ_TIMEOUT;

    private CarbonAwareConfiguration() {
    }

    /**
     * This returns the default carbon aware configuration to schedule jobs with the least amount of Carbon emissions
     *
     * @return the default CarbonAware configuration
     */
    public static CarbonAwareConfiguration usingStandardCarbonAwareConfiguration() {
        return new CarbonAwareConfiguration();
    }

    /**
     * Allows to set the area of your datacenter (the area where your application is hosted) in order to have more accurate carbon emissions forecasts
     *
     * @param area a 2-character country code (ISO 3166-1 alpha-2) or an ENTSO-E area code.
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareConfiguration andArea(String area) {
        this.area = area;
        return this;
    }

    public CarbonAwareConfiguration andState(String state) {
        this.state = state;
        return this;
    }

    public CarbonAwareConfiguration andCloudProvider(String cloudProvider, String cloudRegion) {
        this.cloudProvider = cloudProvider;
        this.cloudRegion = cloudRegion;
        return this;
    }

    public CarbonAwareConfiguration andCarbonAwareApiUrl(String carbonAwareApiUrl) {
        this.carbonAwareApiUrl = carbonAwareApiUrl;
        return this;
    }

    public CarbonAwareConfiguration andApiClientConnectTimeout(Duration apiClientConnectTimeout) {
        this.apiClientConnectTimeout = apiClientConnectTimeout;
        return this;
    }

    public CarbonAwareConfiguration andApiClientReadTimeout(Duration apiClientReadTimeout) {
        this.apiClientReadTimeout = apiClientReadTimeout;
        return this;
    }
}
