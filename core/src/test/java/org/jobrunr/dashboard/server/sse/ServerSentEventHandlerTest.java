package org.jobrunr.dashboard.server.sse;

import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerSentEventHandlerTest {

    @Test
    void whenConnectionLimitIsExceededTheExchangeIsRejectedWithoutBodyAndClosed() throws IOException {
        TestServerSentEventHandler handler = new TestServerSentEventHandler();

        for (int i = 0; i <= 40; i++) {
            handler.handle(openSseExchange());
        }

        HttpExchange rejectedExchange = mock(HttpExchange.class);
        handler.handle(rejectedExchange);

        verify(rejectedExchange).sendResponseHeaders(417, -1);
        verify(rejectedExchange).close();
    }

    private HttpExchange openSseExchange() {
        return mock(HttpExchange.class);
    }

    private static class TestServerSentEventHandler extends ServerSentEventHandler {
        @Override
        protected SseExchange createSseExchange(HttpExchange httpExchange) {
            SseExchange sseExchange = mock(SseExchange.class);
            when(sseExchange.isClosed()).thenReturn(false);
            return sseExchange;
        }
    }
}
