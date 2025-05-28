package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.StringUtils;
import org.jobrunr.utils.annotations.VisibleFor;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Optional.ofNullable;
import static org.jobrunr.utils.CollectionUtils.mapOf;

/**
 * Internal class for JobRunr to access all {@link CarbonAwareJobProcessingConfiguration} details
 */
public class CarbonAwareConfigurationReader {
    private final CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration;

    public CarbonAwareConfigurationReader(CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingConfiguration) {
        this.carbonAwareJobProcessingConfiguration = carbonAwareJobProcessingConfiguration;
    }

    public boolean isEnabled() {
        return carbonAwareJobProcessingConfiguration.enabled;
    }

    public String getCarbonIntensityApiUrl() {
        return carbonAwareJobProcessingConfiguration.carbonIntensityApiUrl;
    }

    public static String getCarbonIntensityForecastApiUrl(String hostName) {
        return hostName + getCarbonIntensityForecastApiPath();
    }

    public static String getCarbonIntensityForecastApiPath() {
        return "/carbon-intensity/forecast";
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

    URL getCarbonIntensityForecastApiFullPathUrl() throws MalformedURLException {
        return new URL(getCarbonIntensityForecastApiUrl(getCarbonIntensityApiUrl()) + getCarbonIntensityForecastQueryString());
    }

    @VisibleFor("testing")
    String getCarbonIntensityForecastQueryString() {
        StringJoiner sj = new StringJoiner("&");
        Map<String, String> queryParams = mapOf("region", getAreaCode(), "dataProvider", getDataProvider(), "externalCode", getExternalCode(), "externalIdentifier", getExternalIdentifier());
        queryParams.forEach((key, value) -> ofNullable(value).ifPresent(v -> sj.add(key + "=" + StringUtils.urlEncode(v))));
        return "?" + sj;
    }
}
