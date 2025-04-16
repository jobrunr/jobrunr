package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticFileHttpHandlerTest {

    @Mock
    HttpExchange httpExchange;

    @Mock
    Headers headers;

    StaticFileHttpHandler staticFileHttpHandler;

    @BeforeEach
    void setupHttpExchange() {
        lenient().when(httpExchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(httpExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());

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
    }

    @Test
    void returns404IfStaticFileNotFound() throws IOException {
        when(httpExchange.getRequestURI()).thenReturn(URI.create("/static/unknownimg.jpg"));

        staticFileHttpHandler.handle(httpExchange);

        verify(httpExchange).sendResponseHeaders(404, -1L);
    }
}