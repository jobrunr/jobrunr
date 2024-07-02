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

    public static String getCarbonIntensityDayAheadEnergyPricesApiUrl(String hostName) {
        return hostName + getCarbonIntensityDayAheadEnergyPricesApiPath();
    }

    public static String getCarbonIntensityDayAheadEnergyPricesApiPath() {
        return "/carbon-intensity/day-ahead-energy-prices";
    }

    public String getAreaCode() {
        return carbonAwareConfiguration.areaCode;
    }

    public String getState() {
        return carbonAwareConfiguration.state;
    }

    public Duration getApiClientConnectTimeout() {
        return carbonAwareConfiguration.apiClientConnectTimeout;
    }

    public Duration getApiClientReadTimeout() {
        return carbonAwareConfiguration.apiClientReadTimeout;
    }

    URL getCarbonAwareApiDayAheadEnergyPricesFullPathUrl() throws MalformedURLException {
        return new URL(getCarbonIntensityDayAheadEnergyPricesApiUrl(getCarbonIntensityApiUrl()) + getDayAheadEnergyPricesQueryString());
    }

    private String getDayAheadEnergyPricesQueryString() {
        return "?areaCode=" + ofNullable(getAreaCode()).orElse("")
                + "&state=" + ofNullable(getState()).orElse("");
    }
}
