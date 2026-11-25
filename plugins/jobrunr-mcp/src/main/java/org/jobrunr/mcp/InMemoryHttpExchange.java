package org.jobrunr.mcp;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * An {@link HttpExchange} without a socket, so the dashboard's REST handlers can answer a request in memory.
 */
class InMemoryHttpExchange extends HttpExchange {

    static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;

    private final String requestMethod;
    private final URI requestUri;
    private final Headers responseHeaders = new Headers();
    private final LimitedOutput responseBody = new LimitedOutput();
    private int responseCode;

    InMemoryHttpExchange(String requestMethod, String requestUri) {
        this.requestMethod = requestMethod;
        this.requestUri = URI.create(requestUri);
    }

    String getResponseBodyAsString() {
        return responseBody.toString(UTF_8);
    }

    boolean responseExceededLimit() {
        return responseBody.exceeded;
    }

    @Override
    public Headers getRequestHeaders() {
        return new Headers();
    }

    @Override
    public Headers getResponseHeaders() {
        return responseHeaders;
    }

    @Override
    public URI getRequestURI() {
        return requestUri;
    }

    @Override
    public String getRequestMethod() {
        return requestMethod;
    }

    @Override
    public HttpContext getHttpContext() {
        return null;
    }

    @Override
    public void close() {
        // nothing to close
    }

    @Override
    public InputStream getRequestBody() {
        return InputStream.nullInputStream();
    }

    @Override
    public OutputStream getResponseBody() {
        return responseBody;
    }

    @Override
    public void sendResponseHeaders(int responseCode, long responseLength) {
        this.responseCode = responseCode;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public String getProtocol() {
        return "HTTP/1.1";
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public void setAttribute(String name, Object value) {
        // no attributes
    }

    @Override
    public void setStreams(InputStream inputStream, OutputStream outputStream) {
        // the streams live in memory
    }

    @Override
    public HttpPrincipal getPrincipal() {
        return null;
    }

    private static final class LimitedOutput extends ByteArrayOutputStream {
        private boolean exceeded;

        @Override
        public synchronized void write(int value) {
            ensureCapacityFor(1);
            super.write(value);
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) {
            ensureCapacityFor(length);
            super.write(bytes, offset, length);
        }

        private void ensureCapacityFor(int additionalBytes) {
            if (additionalBytes < 0 || additionalBytes > MAX_RESPONSE_BYTES - size()) {
                exceeded = true;
                throw new ResponseLimitExceededException();
            }
        }
    }

    static final class ResponseLimitExceededException extends RuntimeException {
        ResponseLimitExceededException() {
            super("Dashboard response exceeded the MCP connector limit");
        }
    }
}
