package org.jobrunr.dashboard.server.http;

import org.jobrunr.dashboard.server.http.url.RequestUrl;
import org.jspecify.annotations.Nullable;

public class HttpRequest {

    private final RequestUrl requestUrl;

    public HttpRequest(RequestUrl requestUrl) {
        this.requestUrl = requestUrl;
    }

    public @Nullable String param(String paramName) {
        return requestUrl.param(paramName);
    }

    public <T> T param(String paramName, Class<T> clazz) {
        return requestUrl.param(paramName, clazz);
    }

    public <T> T fromQueryParams(Class<T> clazz) {
        return requestUrl.fromQueryParams(clazz);
    }

    public <T> @Nullable T queryParam(String queryParamName, Class<T> clazz, @Nullable T defaultValue) {
        return requestUrl.queryParam(queryParamName, clazz, defaultValue);
    }
}
