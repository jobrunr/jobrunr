package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.http.url.TeenyRequestUrl;
import org.jobrunr.utils.mapper.JsonMapper;

public class HttpRequest {

    private final TeenyRequestUrl requestUrl;
    private final HttpExchange httpExchange;
    private final JsonMapper jsonMapper;

    public HttpRequest(TeenyRequestUrl requestUrl, HttpExchange httpExchange, JsonMapper jsonMapper) {
        this.requestUrl = requestUrl;
        this.httpExchange = httpExchange;
        this.jsonMapper = jsonMapper;
    }

    public String param(String paramName) {
        return requestUrl.param(paramName);
    }

    public <T> T param(String paramName, Class<T> clazz) {
        return requestUrl.param(paramName, clazz);

    }

    public <T> T fromQueryParams(Class<T> clazz) {
        return requestUrl.fromQueryParams(clazz);
    }

    public <T> T queryParam(String queryParamName, Class<T> clazz, T defaultValue) {
        return requestUrl.queryParam(queryParamName, clazz, defaultValue);
    }
}
