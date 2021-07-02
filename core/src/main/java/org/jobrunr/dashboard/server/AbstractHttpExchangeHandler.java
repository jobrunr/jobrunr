package org.jobrunr.dashboard.server;

public abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler {

    @Override
    public void close() {
        // default to no-op
    }

}
