package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.AbstractTeenyHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.ExceptionHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestMethodHandlers;
import org.jobrunr.dashboard.server.http.url.TeenyMatchUrl;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.jobrunr.dashboard.server.http.handlers.HttpRequestHandlers.RequestMethod.*;

public class RestHttpHandler extends AbstractTeenyHttpHandler {

    private final String contextPath;
    private final JsonMapper jsonMapper;
    private final HttpRequestHandlers requestHandlers;
    private final Map<Class<? extends Exception>, ExceptionHandler> exceptionHandlers;

    public RestHttpHandler(String contextPath, JsonMapper jsonMapper) {
        this.contextPath = contextPath;
        this.jsonMapper = jsonMapper;
        this.requestHandlers = new HttpRequestHandlers();
        this.exceptionHandlers = new HashMap<>();
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    public void get(String url, HttpRequestHandler httpRequestHandler) {
        requestHandlers.get(GET).put(url, httpRequestHandler);
    }

    public void put(String url, HttpRequestHandler httpRequestHandler) {
        requestHandlers.get(PUT).put(url, httpRequestHandler);
    }

    public void post(String url, HttpRequestHandler httpRequestHandler) {
        requestHandlers.get(POST).put(url, httpRequestHandler);
    }

    public void delete(String url, HttpRequestHandler httpRequestHandler) {
        requestHandlers.get(OPTIONS).put(url, HttpRequestHandlers.ok);
        requestHandlers.get(DELETE).put(url, httpRequestHandler);
    }

    public void head(String url, HttpRequestHandler httpRequestHandler) {
        requestHandlers.get(HEAD).put(url, httpRequestHandler);
    }

    public <T extends Exception> void withExceptionMapping(Class<T> clazz, ExceptionHandler<T> exceptionHandler) {
        exceptionHandlers.put(clazz, exceptionHandler);
    }

    @Override
    public void handle(HttpExchange httpExchange) {
        TeenyMatchUrl actualUrl = new TeenyMatchUrl(httpExchange.getRequestURI().toString().replace(contextPath, ""));

        HttpRequestMethodHandlers httpRequestMethodHandlers = requestHandlers.get(httpExchange.getRequestMethod());
        Optional<String> matchingUrl = httpRequestMethodHandlers.findMatchingUrl(actualUrl);
        if (matchingUrl.isPresent()) {
            matchingUrl
                    .map(httpRequestMethodHandlers::get)
                    .ifPresent(httpRequestHandler -> processRequest(httpRequestHandler, new HttpRequest(actualUrl.toRequestUrl(matchingUrl.get())), new HttpResponse(httpExchange, jsonMapper)));
        } else {
            processRequest(HttpRequestHandlers.notFound, new HttpRequest(null), new HttpResponse(httpExchange, jsonMapper));
        }
    }

    private void processRequest(HttpRequestHandler httpRequestHandler, HttpRequest httpRequest, HttpResponse httpResponse) {
        try {
            httpRequestHandler.accept(httpRequest, httpResponse);
        } catch (Exception e) {
            if (exceptionHandlers.containsKey(e.getClass())) {
                exceptionHandlers.get(e.getClass()).accept(e, httpResponse);
            } else {
                httpResponse.error(e);
            }
        }
    }
}
