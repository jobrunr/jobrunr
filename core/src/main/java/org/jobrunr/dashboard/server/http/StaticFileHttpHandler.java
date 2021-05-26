package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.AbstractHttpExchangeHandler;
import org.jobrunr.utils.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class StaticFileHttpHandler extends AbstractHttpExchangeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticFileHttpHandler.class);

    private final String contextPath;
    private final String rootDir;
    private final boolean singlePageApp;

    public StaticFileHttpHandler(String contextPath, String rootDir) {
        this(contextPath, rootDir, false);
    }

    public StaticFileHttpHandler(String contextPath, String rootDir, boolean singlePageApp) {
        this.contextPath = contextPath;
        this.rootDir = rootDir;
        this.singlePageApp = singlePageApp;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        try {
            String requestUri = httpExchange.getRequestURI().toString();
            requestUri = sanitizeRequestUri(requestUri);

            final String toServe = requestUri.substring((contextPath + "/").length());
            final URL resource = this.getClass().getClassLoader().getResource(rootDir + toServe);
            if (resource != null) {
                httpExchange.getResponseHeaders().add(ContentType._HEADER_NAME, ContentType.from(toServe));
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                copyResourceToResponseBody(resource, httpExchange);
            } else {
                httpExchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception shouldNotHappen) {
            LOGGER.error("Error serving static files", shouldNotHappen);
        }
    }

    private String sanitizeRequestUri(String requestUri) {
        if(requestUri.contains(".")){
            return requestUri;
        } else if (singlePageApp) {
            return contextPath + "/index.html";
        } else {
            if (requestUri.equals(contextPath)) {
                requestUri += "/index.html";
            } else if (requestUri.equals(contextPath + "/")) {
                requestUri += "index.html";
            }
            return requestUri;
        }
    }

    void copyResourceToResponseBody(URL resource, HttpExchange httpExchange) throws IOException {
        try (InputStream inputStream = resource.openStream(); OutputStream outputStream = httpExchange.getResponseBody()) {
            IOUtils.copyStream(inputStream, outputStream);
        }
    }
}
