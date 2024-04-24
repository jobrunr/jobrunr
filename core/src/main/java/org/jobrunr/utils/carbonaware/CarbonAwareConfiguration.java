package org.jobrunr.utils.carbonaware;

import java.time.Duration;

import static org.jobrunr.utils.carbonaware.CarbonAwareConfigurationReader.getCarbonAwareApiUrl;

public class CarbonAwareConfiguration {

    public static String DEFAULT_CARBON_AWARE_API_URL = getCarbonAwareApiUrl("http://jobrunr-carbon-api-dev.swedencentral.cloudapp.azure.com:10000");
    public static Duration DEFAULT_CLIENT_API_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static Duration DEFAULT_CLIENT_API_READ_TIMEOUT = Duration.ofSeconds(3);

     String carbonAwareApiUrl = DEFAULT_CARBON_AWARE_API_URL;
     String areaCode;
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
     * Allows to set the areaCode of your datacenter (the area where your application is hosted) in order to have more accurate carbon emissions forecasts
     *
     * @param areaCode a 2-character country code (ISO 3166-1 alpha-2) or an ENTSO-E area code.
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareConfiguration andAreaCode(String areaCode) {
        this.areaCode = areaCode;
        return this;
    }

    /**
     * For US. Allows to set the state of your datacenter (the state where your application is hosted) in order to have more accurate carbon emissions forecasts
     */
    public CarbonAwareConfiguration andState(String state) {
        this.state = state;
        return this;
    }

    /**
     * Allows to set the cloud provider and region of your datacenter (the cloud provider and region where your application is hosted) in order to have more accurate carbon emissions forecasts
     */
    public CarbonAwareConfiguration andCloudProvider(String cloudProvider, String cloudRegion) {
        this.cloudProvider = cloudProvider;
        this.cloudRegion = cloudRegion;
        return this;
    }

    /**
     * Allows to set the CarbonAware API URL
     */
    public CarbonAwareConfiguration andCarbonAwareApiUrl(String carbonAwareApiUrl) {
        this.carbonAwareApiUrl = carbonAwareApiUrl;
        return this;
    }

    /**
     * Allows to set the connect timeout for the API client
     */
    public CarbonAwareConfiguration andApiClientConnectTimeout(Duration apiClientConnectTimeout) {
        this.apiClientConnectTimeout = apiClientConnectTimeout;
        return this;
    }

    /**
     * Allows to set the read timeout for the API client
     */
    public CarbonAwareConfiguration andApiClientReadTimeout(Duration apiClientReadTimeout) {
        this.apiClientReadTimeout = apiClientReadTimeout;
        return this;
    }
}
