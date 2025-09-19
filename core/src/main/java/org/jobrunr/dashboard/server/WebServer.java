package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

    private final HttpServer httpServer;
    private final ExecutorService executorService;
    private final Set<HttpExchangeHandler> httpHandlers;

    public WebServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            executorService = Executors.newCachedThreadPool();
            httpServer.setExecutor(executorService);
            httpHandlers = new HashSet<>();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public HttpContext createContext(HttpExchangeHandler httpHandler) {
        httpHandlers.add(httpHandler);
        return httpServer.createContext(httpHandler.getContextPath(), httpHandler);
    }

    public void start() {
        httpServer.start();
    }

    public void stop() {
        httpHandlers.forEach(this::closeHttpHandler);
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        httpServer.stop(0);
    }

    public String getWebServerHostAddress() {
        if (httpServer.getAddress().getAddress().isAnyLocalAddress()) {
            return "localhost";
        }
        return httpServer.getAddress().getAddress().getHostAddress();
    }

    public int getWebServerHostPort() {
        return httpServer.getAddress().getPort();
    }

    private void closeHttpHandler(HttpExchangeHandler httpHandler) {
        try {
            httpHandler.close();
        } catch (Exception shouldNotHappen) {
            LOGGER.warn("Error closing HttpHandler", shouldNotHappen);
        }
    }
}
