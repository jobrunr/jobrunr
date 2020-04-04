package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.TeenyHttpHandler;
import org.jobrunr.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class StaticFileHttpHandler implements TeenyHttpHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticFileHttpHandler.class);

    private final String contextPath;
    private final Path rootDir;
    private final boolean singlePageApp;

    public StaticFileHttpHandler(String contextPath, String rootDir) {
        this(contextPath, rootDir, false);
    }

    public StaticFileHttpHandler(String contextPath, String rootDir, boolean singlePageApp) {
        this(contextPath, PathUtils.getResourcesPath(rootDir), singlePageApp);
    }

    public StaticFileHttpHandler(String contextPath, Path rootDir, boolean singlePageApp) {
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

            final Path toServe = rootDir.resolve(requestUri.substring((contextPath + "/").length()));
            if (Files.exists(toServe)) {
                httpExchange.getResponseHeaders().add(ContentType._HEADER_NAME, ContentType.from(toServe));
                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                httpExchange.sendResponseHeaders(200, 0);
                final OutputStream responseBody = httpExchange.getResponseBody();
                Files.copy(toServe, responseBody);
                responseBody.close();
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
}
