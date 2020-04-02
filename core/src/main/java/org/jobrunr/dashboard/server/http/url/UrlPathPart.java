package org.jobrunr.dashboard.server.http.url;

public interface UrlPathPart {

    boolean matches(UrlPathPart pathPart);

    String part();
}
