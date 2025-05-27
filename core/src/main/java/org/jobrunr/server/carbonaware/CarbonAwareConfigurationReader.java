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
 * Internal class for JobRunr to access all {@link CarbonAwareConfiguration} details
 */
public class CarbonAwareConfigurationReader {
    private final CarbonAwareConfiguration carbonAwareConfiguration;

    public CarbonAwareConfigurationReader(CarbonAwareConfiguration carbonAwareConfiguration) {
        this.carbonAwareConfiguration = carbonAwareConfiguration;
    }

    public boolean isEnabled() {
        return carbonAwareConfiguration.enabled;
    }

    public String getCarbonIntensityApiUrl() {
        return carbonAwareConfiguration.carbonIntensityApiUrl;
    }

    public static String getCarbonIntensityForecastApiUrl(String hostName) {
        return hostName + getCarbonIntensityForecastApiPath();
    }

    public static String getCarbonIntensityForecastApiPath() {
        return "/carbon-intensity/forecast";
    }

    public String getDataProvider() {
        return carbonAwareConfiguration.dataProvider;
    }

    public String getAreaCode() {
        return carbonAwareConfiguration.areaCode;
    }

    public String getExternalCode() {
        return carbonAwareConfiguration.externalCode;
    }

    public String getExternalIdentifier() {
        return carbonAwareConfiguration.externalIdentifier;
    }

    public int getApiClientRetriesOnException() {
        return carbonAwareConfiguration.apiClientRetriesOnException;
    }

    public Duration getApiClientConnectTimeout() {
        return carbonAwareConfiguration.apiClientConnectTimeout;
    }

    public Duration getApiClientReadTimeout() {
        return carbonAwareConfiguration.apiClientReadTimeout;
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
