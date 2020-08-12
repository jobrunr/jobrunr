package org.jobrunr.dashboard.server;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeenyWebServerTest {

    @Test
    void httpHandlersAreClosedWhenWebserverIsStopped() {
        // GIVEN
        final TeenyHttpHandler teenyHttpHandlerMock = Mockito.mock(TeenyHttpHandler.class);
        when(teenyHttpHandlerMock.getContextPath()).thenReturn("/some-context-path");

        final TeenyWebServer teenyWebServer = new TeenyWebServer(8000);
        teenyWebServer.createContext(teenyHttpHandlerMock);

        // WHEN
        teenyWebServer.stop();

        // THEN
        verify(teenyHttpHandlerMock).close();
    }

}