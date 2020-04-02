package org.jobrunr.dashboard.server.http.handlers;

import java.util.HashMap;

public class HttpRequestHandlers extends HashMap<String, HttpRequestMethodHandlers> {

    public static final HttpRequestHandler ok = (httpRequest, httpResponse) -> httpResponse.statusCode(200);
    public static final HttpRequestHandler notFound = (httpRequest, httpResponse) -> httpResponse.statusCode(404);

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

    public HttpRequestMethodHandlers get(String method) {
        if (!super.containsKey(method)) {
            put(method, new HttpRequestMethodHandlers());
        }
        return super.get(method);
    }
}
