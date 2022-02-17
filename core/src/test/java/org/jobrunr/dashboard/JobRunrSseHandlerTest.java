package org.jobrunr.dashboard;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Timer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrSseHandlerTest {

    private JobRunrSseHandler jobRunrSseHandler;
    private InMemoryStorageProvider storageProvider;

    @BeforeEach
    void setUpSseHandler() {
        storageProvider = Mockito.spy(new InMemoryStorageProvider());
        jobRunrSseHandler = new JobRunrSseHandler(storageProvider, new JacksonJsonMapper());
    }

    @Test
    void onCloseOfSseHandlersAllSseExchangesAreClosed() throws IOException {
        final HttpExchange httpExchange1 = createHttpExchangeMock();
        when(httpExchange1.getRequestURI()).thenReturn(URI.create("/sse/jobstats"));

        final HttpExchange httpExchange2 = createHttpExchangeMock();
        when(httpExchange2.getRequestURI()).thenReturn(URI.create("/sse/jobstats"));

        jobRunrSseHandler.handle(httpExchange1);
        jobRunrSseHandler.handle(httpExchange2);
        final Timer timerBeforeClosingSseHandler = getInternalState(storageProvider, "timer");
        assertThat(timerBeforeClosingSseHandler).isNotNull();

        jobRunrSseHandler.close();

        verify(storageProvider, times(2)).removeJobStorageOnChangeListener(any());
        final Timer timerAfterClosingSseHandler = getInternalState(storageProvider, "timer");
        assertThat(timerAfterClosingSseHandler).isNull();
    }

    private HttpExchange createHttpExchangeMock() {
        final HttpExchange httpExchange = mock(HttpExchange.class);
        when(httpExchange.getResponseBody()).thenReturn(new ByteArrayOutputStream());
        when(httpExchange.getResponseHeaders()).thenReturn(new Headers());
        return httpExchange;
    }
}