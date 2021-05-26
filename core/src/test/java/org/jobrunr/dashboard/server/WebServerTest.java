package org.jobrunr.dashboard.server;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebServerTest {

    @Test
    void httpHandlersAreClosedWhenWebserverIsStopped() {
        // GIVEN
        final HttpExchangeHandler httpExchangeHandlerMock = Mockito.mock(HttpExchangeHandler.class);
        when(httpExchangeHandlerMock.getContextPath()).thenReturn("/some-context-path");

        final WebServer webServer = new WebServer(8000);
        webServer.createContext(httpExchangeHandlerMock);

        // WHEN
        webServer.stop();

        // THEN
        verify(httpExchangeHandlerMock).close();
    }

}