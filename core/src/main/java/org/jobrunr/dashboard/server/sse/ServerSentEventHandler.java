package org.jobrunr.dashboard.server.sse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.TeenyHttpHandler;
import org.jobrunr.utils.resilience.RateLimiter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.jobrunr.utils.resilience.RateLimiter.Builder.rateLimit;
import static org.jobrunr.utils.resilience.RateLimiter.SECOND;

public class ServerSentEventHandler implements TeenyHttpHandler {

    private final String contextPath;
    private final RateLimiter rateLimiter;
    private final Set<SseExchange> sseExchanges;
    private String lastSentMessage;

    public ServerSentEventHandler() {
        this("/sse");
    }

    public ServerSentEventHandler(String contextPath) {
        this.contextPath = contextPath;
        this.rateLimiter = rateLimit().at2Requests().per(SECOND);
        this.sseExchanges = ConcurrentHashMap.newKeySet();
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        SseExchange e = new SseExchange(httpExchange);
        sseExchanges.add(e);
        e.sendMessage(lastSentMessage);
        subscribersChanged(sseExchanges.size());
    }

    protected void subscribersChanged(int amount) {

    }

    public boolean hasSubscribers() {
        return !hasNoSubscribers();
    }

    public boolean hasNoSubscribers() {
        return sseExchanges.isEmpty();
    }

    public void emitMessage(String message) {
        if (message == null) return;
        if (message.equals(lastSentMessage)) return;
        if (rateLimiter.isRateLimited()) return;

        sseExchanges.forEach(sseExchange -> sseExchange.sendMessage(message));
        final boolean hasRemovedSubscribers = sseExchanges.removeIf(SseExchange::isStale);
        if (hasRemovedSubscribers) subscribersChanged(sseExchanges.size());
        this.lastSentMessage = message;
    }

    private static class SseExchange {
        private final BufferedWriter writer;
        private boolean inError;

        public SseExchange(HttpExchange httpExchange) throws IOException {
            this.writer = new BufferedWriter(new OutputStreamWriter(httpExchange.getResponseBody()));
            Headers responseHeaders = httpExchange.getResponseHeaders();
            responseHeaders.add("Cache-Control", "no-cache,public");
            responseHeaders.add("Content-Type", "text/event-stream");
            responseHeaders.add("Connection", "keep-alive");
            responseHeaders.add("Language", "en-US");
            responseHeaders.add("Charset", "UTF-8");
            responseHeaders.add("Access-Control-Allow-Origin", "*");
            httpExchange.sendResponseHeaders(200, 0);
            this.writer.write("\n\n");
        }

        public boolean isStale() {
            return inError;
        }

        public void sendMessage(String message) {
            if (message == null) return;
            try {
                writer.write("event\n");
                writer.write("data: " + message + "\n\n");
                writer.flush();
            } catch (IOException e) {
                inError = true;
            }
        }
    }
}