package org.jobrunr.server.carbonaware;

public class CarbonIntensityApiClientException extends RuntimeException {

    private final int apiErrorCode;
    private final String apiMessage;

    public CarbonIntensityApiClientException(int errorCode, String message) {
        super(String.format("Carbon Aware API call resulted in an error with code: '%s' and message: '%s'", errorCode, message));
        this.apiErrorCode = errorCode;
        this.apiMessage = message;
    }

    public int getApiErrorCode() {
        return apiErrorCode;
    }

    public String getApiMessage() {
        return apiMessage;
    }
}
