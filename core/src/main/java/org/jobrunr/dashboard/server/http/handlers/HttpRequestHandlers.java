package org.jobrunr.dashboard.server.http.handlers;

import java.util.HashMap;
import java.util.Map;

import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.DELETE;
import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.GET;
import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.HEAD;
import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.OPTIONS;
import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.POST;
import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.PUT;

public class HttpRequestHandlers {

    public static final HttpRequestHandler OK = (httpRequest, httpResponse) -> httpResponse.statusCode(200);
    public static final HttpRequestHandler NOT_FOUND = (httpRequest, httpResponse) -> httpResponse.statusCode(404);

    private final Map<String, HttpRequestMethodHandlers> requestHandlers = new HashMap<>();

    public static class RequestMethod {

        private RequestMethod() {
        }

        public static final String GET = "GET";
        public static final String POST = "POST";
        public static final String PUT = "PUT";
        public static final String DELETE = "DELETE";
        public static final String HEAD = "HEAD";
        public static final String OPTIONS = "OPTIONS";
    }

    public void get(String url, HttpRequestHandler httpRequestHandler) {
        getAllRequestMethodHandlers(GET).put(url, httpRequestHandler);
    }

    public void put(String url, HttpRequestHandler httpRequestHandler) {
        getAllRequestMethodHandlers(PUT).put(url, httpRequestHandler);
    }

    public void post(String url, HttpRequestHandler httpRequestHandler) {
        getAllRequestMethodHandlers(POST).put(url, httpRequestHandler);
    }

    public void delete(String url, HttpRequestHandler httpRequestHandler) {
        getAllRequestMethodHandlers(OPTIONS).put(url, HttpRequestHandlers.OK);
        getAllRequestMethodHandlers(DELETE).put(url, httpRequestHandler);
    }

    public void head(String url, HttpRequestHandler httpRequestHandler) {
        getAllRequestMethodHandlers(HEAD).put(url, httpRequestHandler);
    }

    public HttpRequestMethodHandlers getAllRequestMethodHandlers(String method) {
        return requestHandlers.computeIfAbsent(method.toUpperCase(), m -> new HttpRequestMethodHandlers());
    }
}
