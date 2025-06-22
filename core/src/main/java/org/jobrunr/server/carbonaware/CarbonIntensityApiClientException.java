package org.jobrunr.server.carbonaware;

public class CarbonIntensityApiClientException extends RuntimeException {
    public CarbonIntensityApiClientException(String message) {
        super(message);
    }

    public CarbonIntensityApiClientException(int errorCode, String message) {
        this(String.format("Carbon Aware API call resulted in an error with code: '%s' and message: '%s'", errorCode, message));
    }
}
