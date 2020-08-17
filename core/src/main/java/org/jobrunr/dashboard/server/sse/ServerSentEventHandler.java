package org.jobrunr.dashboard.server.sse;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.AbstractTeenyHttpHandler;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class ServerSentEventHandler extends AbstractTeenyHttpHandler {

    private final String contextPath;
    private final Set<SseExchange> sseExchanges;

    public ServerSentEventHandler() {
        this("/sse");
    }

    public ServerSentEventHandler(String contextPath) {
        this.contextPath = contextPath;
        this.sseExchanges = new HashSet<>();
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (sseExchanges.size() > 40) {
            httpExchange.sendResponseHeaders(417, 0);
            return;
        }
        sseExchanges.add(createSseExchange(httpExchange));
    }

    protected abstract SseExchange createSseExchange(HttpExchange httpExchange) throws IOException;

    @Override
    public void close() {
        sseExchanges.forEach(SseExchange::close);
    }
}