package org.jobrunr.jobs.carbonaware;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import static java.util.Optional.ofNullable;

/**
 * Internal class for JobRunr to access all {@link CarbonAwareConfiguration} details
 */
public class CarbonAwareConfigurationReader {
    private final CarbonAwareConfiguration carbonAwareConfiguration;

    public CarbonAwareConfigurationReader(CarbonAwareConfiguration carbonAwareConfiguration) {
        this.carbonAwareConfiguration = carbonAwareConfiguration;
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

    public String getAreaCode() {
        return carbonAwareConfiguration.areaCode;
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

    private String getCarbonIntensityForecastQueryString() {
        return "?" + ofNullable(getAreaCode()).map(r -> "region=" + r).orElse("");
    }
}
