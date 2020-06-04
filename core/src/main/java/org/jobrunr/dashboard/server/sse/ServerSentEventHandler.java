package org.jobrunr.dashboard.server.sse;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.TeenyHttpHandler;

import java.io.IOException;

public abstract class ServerSentEventHandler implements TeenyHttpHandler {

    private final String contextPath;

    public ServerSentEventHandler() {
        this("/sse");
    }

    public ServerSentEventHandler(String contextPath) {
        this.contextPath = contextPath;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        createSseExchange(httpExchange);
    }

    protected abstract SseExchange createSseExchange(HttpExchange httpExchange) throws IOException;

}