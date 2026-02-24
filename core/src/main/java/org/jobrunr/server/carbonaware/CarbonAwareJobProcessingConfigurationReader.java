package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.StringUtils;
import org.jobrunr.utils.annotations.VisibleFor;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Optional.ofNullable;
import static org.jobrunr.utils.CollectionUtils.mapOf;

/**
 * Internal class for JobRunr to access all {@link CarbonAwareJobProcessingConfiguration} details
 */
public class CarbonAwareJobProcessingConfigurationReader {
    private final CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration;
    static final String CARBON_INTENSITY_FORECAST_API_ROOT_URL = "/carbon-intensity/forecast";

    public CarbonAwareJobProcessingConfigurationReader(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        this.carbonAwareJobProcessingConfiguration = carbonAwareJobProcessingConfiguration;
    }

    public boolean isEnabled() {
        return carbonAwareJobProcessingConfiguration.enabled;
    }

    public String getCarbonIntensityApiBaseUrl() {
        return carbonAwareJobProcessingConfiguration.carbonIntensityApiUrl;
    }

    public static String getCarbonIntensityForecastApiRootUrl(String baseUrl) {
        return baseUrl + CARBON_INTENSITY_FORECAST_API_ROOT_URL;
    }

    public Duration getPollInterval() {
        return carbonAwareJobProcessingConfiguration.pollInterval;
    }

    URL getCarbonIntensityForecastApiFullPathUrl() throws MalformedURLException {
        return URI.create(getCarbonIntensityApiBaseUrl() + getCarbonIntensityForecastQueryString()).toURL();
    }

    public String getDataProvider() {
        return carbonAwareJobProcessingConfiguration.dataProvider;
    }

    public String getAreaCode() {
        return carbonAwareJobProcessingConfiguration.areaCode;
    }

    public String getExternalCode() {
        return carbonAwareJobProcessingConfiguration.externalCode;
    }

    public String getExternalIdentifier() {
        return carbonAwareJobProcessingConfiguration.externalIdentifier;
    }

    public int getApiClientRetriesOnException() {
        return carbonAwareJobProcessingConfiguration.apiClientRetriesOnException;
    }

    public Duration getApiClientConnectTimeout() {
        return carbonAwareJobProcessingConfiguration.apiClientConnectTimeout;
    }

    public Duration getApiClientReadTimeout() {
        return carbonAwareJobProcessingConfiguration.apiClientReadTimeout;
    }

    @VisibleFor("testing")
    String getCarbonIntensityForecastQueryString() {
        StringJoiner sj = new StringJoiner("&");
        Map<String, String> queryParams = mapOf("areaCode", getAreaCode(), "dataProvider", getDataProvider(), "externalCode", getExternalCode(), "externalIdentifier", getExternalIdentifier());
        queryParams.forEach((key, value) -> ofNullable(value).ifPresent(v -> sj.add(key + "=" + StringUtils.urlEncode(v))));
        return "?" + sj;
    }
}
