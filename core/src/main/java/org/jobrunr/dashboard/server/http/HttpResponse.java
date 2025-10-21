package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.utils.exceptions.Exceptions.ThrowingConsumer;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class HttpResponse {

    private final HttpExchange httpExchange;
    private final JsonMapper jsonMapper;

    public HttpResponse(HttpExchange httpExchange, JsonMapper jsonMapper) {
        this.httpExchange = httpExchange;
        this.jsonMapper = jsonMapper;

    }

    public HttpResponse fromJsonString(String responseAsString) {
        return data(ContentType.APPLICATION_JSON, outputStream -> outputStream.write(responseAsString.getBytes(StandardCharsets.UTF_8)));
    }

    public HttpResponse asJson(Object object) {
        return data(ContentType.APPLICATION_JSON, outputStream -> jsonMapper.serialize(outputStream, object));
    }

    public HttpResponse error(Throwable t) {
        data(500, ContentType.TEXT_PLAIN, stream -> t.printStackTrace(new PrintStream(stream)));
        return this;
    }

    private HttpResponse data(String contentType, ThrowingConsumer<OutputStream> streamConsumer) {
        data(200, contentType, streamConsumer);
        return this;
    }

    private HttpResponse data(int status, String contentType, ThrowingConsumer<OutputStream> streamConsumer) {
        httpExchange.getResponseHeaders().add(ContentType._HEADER_NAME, contentType);
        try (OutputStream outputStream = httpExchange.getResponseBody()) {
            httpExchange.sendResponseHeaders(status, 0);
            streamConsumer.accept(outputStream);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return this;
    }

    public void statusCode(int i) {
        try {
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            httpExchange.sendResponseHeaders(i, -1);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
