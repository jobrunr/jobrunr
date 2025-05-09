package org.jobrunr.server.carbonaware;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

import static org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader.getCarbonIntensityForecastApiUrl;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class CarbonAwareConfiguration {

    public static String DEFAULT_CARBON_INTENSITY_API_URL = getCarbonIntensityForecastApiUrl("https://api.jobrunr.io");
    public static Duration DEFAULT_CLIENT_API_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static Duration DEFAULT_CLIENT_API_READ_TIMEOUT = Duration.ofSeconds(3);

    boolean enabled = false;
    String carbonIntensityApiUrl = DEFAULT_CARBON_INTENSITY_API_URL;
    String areaCode;
    String dataProvider;
    String externalCode;
    String externalIdentifier;
    Duration apiClientConnectTimeout = DEFAULT_CLIENT_API_CONNECT_TIMEOUT;
    Duration apiClientReadTimeout = DEFAULT_CLIENT_API_READ_TIMEOUT;

    private CarbonAwareConfiguration() {
    }

    /**
     * This returns the default carbon aware configuration to schedule jobs at low carbon emission moments
     *
     * @return the default CarbonAware configuration
     */
    public static CarbonAwareConfiguration usingStandardCarbonAwareConfiguration() {
        return new CarbonAwareConfiguration().andCarbonAwareSchedulingEnabled(true);
    }

    /**
     * This returns the carbon aware configuration that has the carbon aware functionality disabled.
     *
     * @return the disabled CarbonAware configuration
     */
    public static CarbonAwareConfiguration usingDisabledCarbonAwareConfiguration() {
        return new CarbonAwareConfiguration().andCarbonAwareSchedulingEnabled(false);
    }

    /**
     * Allows to enable or disable carbon aware scheduling.
     *
     * @param enabled the status of carbon aware scheduling
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareConfiguration andCarbonAwareSchedulingEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Allows to set your preferred carbon intensity forecast dataProvider.
     *
     * @param dataProvider a carbon intensity data provider (e.g., 'ENTSO-E', 'Azure', etc.)
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareConfiguration andDataProvider(String dataProvider) {
        this.dataProvider = dataProvider;
        return this;
    }

    /**
     * Allows to set the areaCode of your datacenter (the area where your application is hosted) in order to have more accurate carbon emissions forecasts.
     * Unless specified via {@link CarbonAwareConfiguration#andDataProvider(String)}, the forecast may be from any dataProvider that supports the areaCode.
     *
     * @param areaCode a supported area code (e.g., ISO 3166-2 code like 'BE', 'IT-NO', 'US-CA' or a cloud provider region code)
     * @return the same configuration instance which provides a fluent api
     * @throws IllegalStateException if either externalCode or externalIdentifier is specified
     */
    public CarbonAwareConfiguration andAreaCode(String areaCode) {
        validateConfiguration(areaCode, externalCode, externalIdentifier);
        this.areaCode = areaCode;
        return this;
    }

    /**
     * Allows to set the code of an area as defined by your specified dataProvider in order to have more accurate carbon emissions forecasts.
     *
     * @param externalCode the area code as defined by your specified dataProvider (e.g., 'IT-North')
     * @return the same configuration instance which provides a fluent api
     * @throws IllegalStateException if a dataProvider is not specified, or if either areaCode or externalIdentifier is specified
     */
    public CarbonAwareConfiguration andExternalCode(String externalCode) {
        validateConfiguration(areaCode, externalCode, externalIdentifier);
        if (isNullOrEmpty(dataProvider)) {
            throw new IllegalStateException("Please set the dataProvider before setting the externalCode.");
        }
        this.externalCode = externalCode;
        return this;
    }

    /**
     * Allows to set the identifier of an area as defined by your specified dataProvider in order to have more accurate carbon emissions forecasts.
     *
     * @param externalIdentifier the identifier of an area from your specified dataProvider (e.g., '10Y1001A1001A73I')
     * @return the same configuration instance which provides a fluent api
     * @throws IllegalStateException if a dataProvider is not specified
     */
    public CarbonAwareConfiguration andExternalIdentifier(String externalIdentifier) {
        validateConfiguration(areaCode, externalCode, externalIdentifier);
        if (isNullOrEmpty(dataProvider)) {
            throw new IllegalStateException("Please set the dataProvider before setting the externalIdentifier.");
        }
        this.externalIdentifier = externalIdentifier;
        return this;
    }

    /**
     * Allows to set the carbon intensity API URL
     */
    public CarbonAwareConfiguration andCarbonIntensityApiUrl(String carbonIntensityApiUrl) {
        this.carbonIntensityApiUrl = carbonIntensityApiUrl;
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

    private void validateConfiguration(String areaCode, String externalCode, String externalIdentifier) {
        if (Stream.of(areaCode, externalCode, externalIdentifier).filter(Objects::nonNull).count() > 1) {
            throw new IllegalStateException("You can only set either areaCode, externalCode or externalIdentifier.");
        }
    }
}
