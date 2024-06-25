package org.jobrunr.carbonaware;

public class CarbonAwareApiClientException extends RuntimeException {
    public CarbonAwareApiClientException(String message) {
        super(message);
    }

    public CarbonAwareApiClientException(int errorCode, String message) {
        this(String.format("Carbon Aware API call resulted in an error with code: '%s' and message: '%s'", errorCode, message));
    }
}
