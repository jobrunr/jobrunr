package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.utils.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticFileHttpHandlerTest {

    @Mock
    HttpExchange httpExchange;

    @Mock
    Headers headers;

    StaticFileHttpHandler staticFileHttpHandler;
    OutputStream outputStream;

    @BeforeEach
    void setupHttpExchange() {
        lenient().when(httpExchange.getResponseHeaders()).thenReturn(headers);
        outputStream = new ByteArrayOutputStream();
        lenient().when(httpExchange.getResponseBody()).thenReturn(outputStream);

        staticFileHttpHandler = new StaticFileHttpHandler("/dashboard", "dashboard/test/", true);
    }

    @Test
    void servesIndexHtmlIfNoFileRequested() throws IOException {
        when(httpExchange.getRequestURI()).thenReturn(URI.create("/dashboard"));

        staticFileHttpHandler.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(200, 0);
    }

    @Test
    void servesIndexEvenIfRequestParamsContainDot() throws IOException {
        when(httpExchange.getRequestURI()).thenReturn(URI.create("/dashboard/jobs?state=FAILED&jobSignature=java.lang.System.out.println(java.lang.String)"));

        staticFileHttpHandler.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(200, 0);
    }

    @Test
    void servesRequestedFile() throws IOException {
        when(httpExchange.getRequestURI()).thenReturn(URI.create("/dashboard/test.html"));

        staticFileHttpHandler.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(200, 0);
        assertThat(outputStream.toString()).isEqualTo(getResourceContent("dashboard/test/test.html"));
    }

    @Test
    void returns404IfStaticFileNotFound() throws IOException {
        when(httpExchange.getRequestURI()).thenReturn(URI.create("/static/unknownimg.jpg"));

        staticFileHttpHandler.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(404, -1L);
    }

    private String getResourceContent(String resourcePath) throws IOException {
        URL resource = this.getClass().getClassLoader().getResource(resourcePath);
        if (resource == null) return null;
        try (InputStream inputStream = resource.openStream(); OutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copyStream(inputStream, outputStream);
            return outputStream.toString();
        }
    }
}