package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HttpResponseTest {

    @Mock
    private HttpExchange httpExchange;
    @Mock
    private Headers headers;

    private ByteArrayOutputStream outputStream;

    @Mock
    private JsonMapper jsonMapper;

    private HttpResponse httpResponse;

    @BeforeEach
    void setUpHttpResponse() {
        lenient().when(httpExchange.getResponseHeaders()).thenReturn(headers);
        outputStream = new ByteArrayOutputStream();
        lenient().when(httpExchange.getResponseBody()).thenReturn(outputStream);
        httpResponse = new HttpResponse(httpExchange, jsonMapper);
    }

    @Test
    void errorRespondsWith500() throws IOException {
        httpResponse.error(new Exception("boom"));

        verify(httpExchange).sendResponseHeaders(500, 0);
    }

    @Test
    void errorDoesNotLeakTheStackTraceInTheResponseBody() {
        httpResponse.error(new IllegalStateException("sensitive internal detail"));

        assertThat(outputStream.toString())
                .doesNotContain("sensitive internal detail")
                .doesNotContain("IllegalStateException")
                .doesNotContain("org.jobrunr");
    }

}