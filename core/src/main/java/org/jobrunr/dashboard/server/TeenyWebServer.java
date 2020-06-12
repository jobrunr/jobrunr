package org.jobrunr.dashboard.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TeenyWebServer {

    private final HttpServer httpServer;
    private final ExecutorService executorService;

    public TeenyWebServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            executorService = Executors.newFixedThreadPool(100);
            httpServer.setExecutor(executorService);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpContext createContext(TeenyHttpHandler httpHandler) {
        return httpServer.createContext(httpHandler.getContextPath(), httpHandler);
    }

    public void start() {
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop() {
        executorService.shutdownNow();
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
}
