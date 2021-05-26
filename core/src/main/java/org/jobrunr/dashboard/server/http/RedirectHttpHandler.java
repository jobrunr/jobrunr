package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.AbstractHttpExchangeHandler;

import java.io.IOException;

public class RedirectHttpHandler extends AbstractHttpExchangeHandler {

    private final String contextPath;
    private final String to;

    public RedirectHttpHandler(String contextPath, String to) {
        this.contextPath = contextPath;
        this.to = to;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        httpExchange.getResponseHeaders().add("Location", to);
        httpExchange.sendResponseHeaders(302, -1);
    }
}
