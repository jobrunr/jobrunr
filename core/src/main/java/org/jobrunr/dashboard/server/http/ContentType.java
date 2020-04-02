package org.jobrunr.dashboard.server.http;

import java.nio.file.Path;

public class ContentType {

    private ContentType() {
    }

    public static final String _HEADER_NAME = "content-type";
    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
    public static final String TEXT_HTML = "text/html;charset=UTF-8";
    public static final String TEXT_PLAIN = "text/plain;charset=UTF-8";
    public static final String TEXT_JAVASCRIPT = "text/javascript;charset=UTF-8";
    public static final String TEXT_CSS = "text/css;charset=UTF-8";
    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_X_ICON = "image/x-icon";

    public static String from(Path path) {
        if (path.toString().toLowerCase().endsWith(".html")) {
            return TEXT_HTML;
        } else if (path.toString().toLowerCase().endsWith(".txt")) {
            return TEXT_PLAIN;
        } else if (path.toString().toLowerCase().endsWith(".json")) {
            return APPLICATION_JSON;
        } else if (path.toString().toLowerCase().endsWith(".js")) {
            return TEXT_JAVASCRIPT;
        } else if (path.toString().toLowerCase().endsWith(".css")) {
            return TEXT_CSS;
        } else if (path.toString().toLowerCase().endsWith(".png")) {
            return IMAGE_PNG;
        } else if (path.toString().toLowerCase().endsWith(".ico")) {
            return IMAGE_X_ICON;
        } else if (path.toString().toLowerCase().endsWith(".map")) {
            return APPLICATION_OCTET_STREAM;
        }
        throw new IllegalStateException("Unsupported mimetype");
    }
}
