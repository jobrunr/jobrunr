package org.jobrunr.server.carbonaware;

import org.jobrunr.server.BackgroundJobServerConfiguration;

import java.time.Duration;
import java.util.Objects;
import java.util.stream.Stream;

import static org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader.getCarbonIntensityForecastApiRootUrl;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

public class CarbonAwareJobProcessingConfiguration {

    public static final String DEFAULT_CARBON_INTENSITY_API_URL = getCarbonIntensityForecastApiRootUrl("https://api.jobrunr.io");
    public static final Duration DEFAULT_API_CLIENT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    public static final Duration DEFAULT_API_CLIENT_READ_TIMEOUT = Duration.ofSeconds(3);
    public static final Integer DEFAULT_API_CLIENT_RETRIES_ON_EXCEPTION = 3;
    public static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMinutes(5);

    boolean enabled = false;
    String carbonIntensityApiUrl = DEFAULT_CARBON_INTENSITY_API_URL;
    String areaCode;
    String dataProvider;
    String externalCode;
    String externalIdentifier;
    Integer apiClientRetriesOnException = DEFAULT_API_CLIENT_RETRIES_ON_EXCEPTION;
    Duration apiClientConnectTimeout = DEFAULT_API_CLIENT_CONNECT_TIMEOUT;
    Duration apiClientReadTimeout = DEFAULT_API_CLIENT_READ_TIMEOUT;
    Duration pollInterval = DEFAULT_POLL_INTERVAL;

    private CarbonAwareJobProcessingConfiguration() {
    }

    /**
     * This returns the default carbon aware configuration to schedule jobs at low carbon emission moments
     *
     * @return the default CarbonAware configuration
     */
    public static CarbonAwareJobProcessingConfiguration usingStandardCarbonAwareJobProcessingConfiguration() {
        return new CarbonAwareJobProcessingConfiguration().andCarbonAwareSchedulingEnabled(true);
    }

    /**
     * Allows to set the pollInterval for carbon aware job processing on the BackgroundJobServer
     * This should typically be in minutes and longer than {@link BackgroundJobServerConfiguration#andPollInterval(Duration)} for tasks because of the nature of carbon aware job procesing.
     *
     * @param pollInterval the pollInterval duration
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareJobProcessingConfiguration andPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
        return this;
    }

    /**
     * Allows to set the pollInterval in minutes for carbon aware job processing on the BackgroundJobServer
     * This can be a much longer interval than {@code pollInterval} for tasks because of the nature of carbon aware job procesing.
     *
     * @param minutes the pollInterval duration in minutes
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareJobProcessingConfiguration andPollIntervalInMinutes(int minutes) {
        return andPollInterval(Duration.ofMinutes(minutes));
    }

    /**
     * This returns the carbon aware configuration that has the carbon aware functionality disabled.
     *
     * @return the disabled CarbonAware configuration
     */
    public static CarbonAwareJobProcessingConfiguration usingDisabledCarbonAwareJobProcessingConfiguration() {
        return new CarbonAwareJobProcessingConfiguration().andCarbonAwareSchedulingEnabled(false);
    }

    /**
     * Allows to enable or disable carbon aware scheduling.
     *
     * @param enabled the status of carbon aware scheduling
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareJobProcessingConfiguration andCarbonAwareSchedulingEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * Allows to set your preferred carbon intensity forecast dataProvider.
     *
     * @param dataProvider a carbon intensity data provider (e.g., 'ENTSO-E', 'Azure', etc.)
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareJobProcessingConfiguration andDataProvider(String dataProvider) {
        this.dataProvider = dataProvider;
        return this;
    }

    /**
     * Allows to set the areaCode of your datacenter (the area where your application is hosted) in order to have more accurate carbon emissions forecasts.
     * Unless specified via {@link CarbonAwareJobProcessingConfiguration#andDataProvider(String)}, the forecast may be from any dataProvider that supports the areaCode.
     *
     * @param areaCode a supported area code (e.g., ISO 3166-2 code like 'BE', 'IT-NO', 'US-CA' or a cloud provider region code)
     * @return the same configuration instance which provides a fluent api
     * @throws IllegalStateException if either externalCode or externalIdentifier is specified
     */
    public CarbonAwareJobProcessingConfiguration andAreaCode(String areaCode) {
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
    public CarbonAwareJobProcessingConfiguration andExternalCode(String externalCode) {
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
    public CarbonAwareJobProcessingConfiguration andExternalIdentifier(String externalIdentifier) {
        validateConfiguration(areaCode, externalCode, externalIdentifier);
        if (isNullOrEmpty(dataProvider)) {
            throw new IllegalStateException("Please set the dataProvider before setting the externalIdentifier.");
        }
        this.externalIdentifier = externalIdentifier;
        return this;
    }

    /**
     * Allows to set the connect timeout for the API client
     */
    public CarbonAwareJobProcessingConfiguration andApiClientConnectTimeout(Duration apiClientConnectTimeout) {
        this.apiClientConnectTimeout = apiClientConnectTimeout;
        return this;
    }

    /**
     * Configures the API client amount of retries when the call throws an exception
     *
     * @param retries the amount of retries (e.g. 3)
     * @return the same configuration instance which provides a fluent api
     */
    public CarbonAwareJobProcessingConfiguration andApiClientRetriesOnException(int retries) {
        this.apiClientRetriesOnException = retries;
        return this;
    }

    /**
     * Allows to set the read timeout for the API client
     */
    public CarbonAwareJobProcessingConfiguration andApiClientReadTimeout(Duration apiClientReadTimeout) {
        this.apiClientReadTimeout = apiClientReadTimeout;
        return this;
    }

    private void validateConfiguration(String areaCode, String externalCode, String externalIdentifier) {
        if (Stream.of(areaCode, externalCode, externalIdentifier).filter(Objects::nonNull).count() > 1) {
            throw new IllegalStateException("You can only set either areaCode, externalCode or externalIdentifier.");
        }
    }
}
