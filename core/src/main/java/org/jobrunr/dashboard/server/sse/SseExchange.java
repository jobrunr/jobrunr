package org.jobrunr.dashboard.server.sse;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class SseExchange implements AutoCloseable {

    private final BufferedWriter writer;

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

    public void sendMessage(String message) {
        if (message == null) return;
        try {
            writer.write("event\n");
            writer.write("data: " + message + "\n\n");
            writer.flush();
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        try {
            writer.write("event: close\n");
            writer.write("data: \n\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            // nothing more we can do...
        }
    }
}
