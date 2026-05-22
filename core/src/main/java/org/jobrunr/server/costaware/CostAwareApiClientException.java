package org.jobrunr.server.costaware;

public class CostAwareApiClientException extends Exception {

    public CostAwareApiClientException(String message, Exception e) {
        super(message, e);
    }

    public CostAwareApiClientException(String message) {
        super(message);
    }
}
