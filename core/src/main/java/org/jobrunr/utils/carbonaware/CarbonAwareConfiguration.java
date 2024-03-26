package org.jobrunr.utils.carbonaware;

public class CarbonAwareConfiguration {
    private static boolean enabled = false;
    private static String carbonAwareApiBaseUrl = "https://jobrunr.io/api/carbon-intensity";
    private static String carbonAwareDayAheadEnergyPricesUrl = "/v1/day-ahead-energy-prices";
    private static String area;
    private static String state;
    private static String cloudProvider;
    private static String cloudRegion;
    private static int apiClientReadTimeout = 3000;
    private static int apiClientConnectTimeout = 3000;
    public CarbonAwareConfiguration(){}

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getArea() {
        return area;
    }

    public static String getState() {
        return state;
    }

    public static String getCloudProvider() {
        return cloudProvider;
    }

    public static String getCloudRegion() {
        return cloudRegion;
    }

    public static int getApiClientReadTimeout() {
        return apiClientReadTimeout;
    }

    public static int getApiClientConnectTimeout() {
        return apiClientConnectTimeout;
    }

    public static String getCarbonAwareApiBaseUrl() {
        return carbonAwareApiBaseUrl;
    }

    public static String getCarbonAwareDayAheadEnergyPricesUrl() {
        return carbonAwareDayAheadEnergyPricesUrl;
    }

    public static void setEnabled(boolean enabled) {
        CarbonAwareConfiguration.enabled = enabled;
    }

    public static void setArea(String area) {
        CarbonAwareConfiguration.area = area;
    }

    public static void setState(String state) {
        CarbonAwareConfiguration.state = state;
    }

    public static void setCloudProvider(String cloudProvider) {
        CarbonAwareConfiguration.cloudProvider = cloudProvider;
    }

    public static void setCloudRegion(String cloudRegion) {
        CarbonAwareConfiguration.cloudRegion = cloudRegion;
    }

    public static void setCarbonAwareApiBaseUrl(String carbonAwareApiBaseUrl) {
        CarbonAwareConfiguration.carbonAwareApiBaseUrl = carbonAwareApiBaseUrl;
    }

    public static void setApiClientReadTimeout(int apiClientReadTimeout) {
        CarbonAwareConfiguration.apiClientReadTimeout = apiClientReadTimeout;
    }

    public static void setApiClientConnectTimeout(int apiClientConnectTimeout) {
        CarbonAwareConfiguration.apiClientConnectTimeout = apiClientConnectTimeout;
    }
}
