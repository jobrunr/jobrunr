package org.jobrunr.dashboard.server;

public abstract class AbstractHttpExchangeHandler implements HttpExchangeHandler {

    public abstract String getContextPath();

    @Override
    public void close() {
        // defalt to no-op
    }

}
