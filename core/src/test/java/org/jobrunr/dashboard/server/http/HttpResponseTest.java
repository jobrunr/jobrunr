package org.jobrunr.dashboard.server.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.OutputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpResponseTest {

    @Mock
    private HttpExchange httpExchange;
    @Mock
    private Headers headers;
    @Mock
    private OutputStream outputStream;

    @Mock
    private JsonMapper jsonMapper;

    private HttpResponse httpResponse;

    @BeforeEach
    void setUpHttpResponse() {
        lenient().when(httpExchange.getResponseHeaders()).thenReturn(headers);
        lenient().when(httpExchange.getResponseBody()).thenReturn(outputStream);
        httpResponse = new HttpResponse(httpExchange, jsonMapper);
    }

    @Test
    void testError() throws IOException {
        httpResponse.error(new Exception());

        verify(httpExchange).sendResponseHeaders(500, 0);
        verify(outputStream, atLeastOnce()).write(any(), anyInt(), anyInt());
    }

}