package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.AbstractHttpExchangeHandler;
import org.jobrunr.utils.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class StaticFileHttpHandler extends AbstractHttpExchangeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticFileHttpHandler.class);
    private static final Set<String> ALLOWED_STATIC_FILE_EXTENSIONS = new HashSet<>(Arrays.asList(".html", ".css", ".js", ".png", ".jpg", ".jpeg", ".webp", ".svg", ".txt", ".json", ".ico"));

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
            String requestPath = httpExchange.getRequestURI().getPath();
            requestPath = sanitizeRequestUri(requestPath);

            final String toServe = requestPath.substring((contextPath + "/").length());
            final URL resource = this.getClass().getClassLoader().getResource(rootDir + toServe);
            if (resource != null) {
                httpExchange.getResponseHeaders().add(ContentType._HEADER_NAME, ContentType.from(toServe));
                if (toServe.endsWith(".html")) {
                    final String nonce = UUID.randomUUID().toString();
                    httpExchange.getResponseHeaders().add("Content-Security-Policy", "script-src 'nonce-" + nonce + "' 'strict-dynamic';object-src 'none';base-uri 'none'");
                    httpExchange.sendResponseHeaders(200, 0);
                    copyHtmlResourceToResponseBody(resource, httpExchange, nonce);
                } else {
                    httpExchange.getResponseHeaders().add("Cache-Control", "public, max-age=604800");
                    httpExchange.getResponseHeaders().add("Content-Security-Policy", "base-uri 'none'; default-src 'none'");
                    httpExchange.sendResponseHeaders(200, 0);
                    copyResourceToResponseBody(resource, httpExchange);
                }
            } else {
                httpExchange.sendResponseHeaders(404, -1);
            }
        } catch (Exception shouldNotHappen) {
            LOGGER.error("Error serving static files", shouldNotHappen);
        }
    }

    private String sanitizeRequestUri(String requestPath) {
        if (isStaticFile(requestPath)) {
            return requestPath;
        } else if (singlePageApp) {
            return contextPath + "/index.html";
        } else {
            if (requestPath.equals(contextPath)) {
                requestPath += "/index.html";
            } else if (requestPath.equals(contextPath + "/")) {
                requestPath += "index.html";
            }
            return requestPath;
        }
    }

    void copyHtmlResourceToResponseBody(URL resource, HttpExchange httpExchange, String nonce) throws IOException {
        try (InputStream inputStream = resource.openStream(); StringWriter stringWriter = new StringWriter(); OutputStream outputStream = httpExchange.getResponseBody()) {
            IOUtils.copyStream(inputStream, stringWriter);
            String result = stringWriter.toString()
                    .replace("%CSP_NONCE%", nonce);
            IOUtils.copyToStream(result, outputStream);
        }
    }

    void copyResourceToResponseBody(URL resource, HttpExchange httpExchange) throws IOException {
        try (InputStream inputStream = resource.openStream(); OutputStream outputStream = httpExchange.getResponseBody()) {
            IOUtils.copyStream(inputStream, outputStream);
        }
    }

    private boolean isStaticFile(String path) {
        return path.contains("/static/") || hasStaticFileExtension(path);
    }

    private boolean hasStaticFileExtension(String path) {
        int extensionIndex = path.lastIndexOf('.');
        if (extensionIndex == -1) return false;
        return ALLOWED_STATIC_FILE_EXTENSIONS.contains(path.substring(extensionIndex));
    }
}
