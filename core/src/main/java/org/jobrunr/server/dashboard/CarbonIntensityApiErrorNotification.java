package org.jobrunr.server.dashboard;

import org.jobrunr.server.carbonaware.CarbonIntensityForecast;

public class CarbonIntensityApiErrorNotification implements DashboardNotification {

    private CarbonIntensityForecast.ApiResponseStatus apiResponseStatus;

    public CarbonIntensityApiErrorNotification(CarbonIntensityForecast.ApiResponseStatus apiResponseStatus) {
        this.apiResponseStatus = apiResponseStatus;
    }

    public CarbonIntensityForecast.ApiResponseStatus getApiResponseStatus() {
        return apiResponseStatus;
    }
}
