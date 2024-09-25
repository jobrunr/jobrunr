package org.jobrunr.quarkus.autoconfigure.server;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrBackgroundJobServerStarterTest {

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Mock
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    Instance<BackgroundJobServer> backgroundJobServerInstance;

    @Mock
    BackgroundJobServer backgroundJobServer;

    JobRunrBackgroundJobServerStarter jobRunrBackgroundJobServerStarter;

    @BeforeEach
    void setup() {
        when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerConfiguration);

        lenient().when(backgroundJobServerInstance.get()).thenReturn(backgroundJobServer);

        jobRunrBackgroundJobServerStarter = new JobRunrBackgroundJobServerStarter(jobRunrRuntimeConfiguration, backgroundJobServerInstance);
    }

    @Test
    void jobRunrStarterStopsBackgroundJobServerIfConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);

        jobRunrBackgroundJobServerStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServer).stop();
    }

    @Test
    void jobRunrStarterDoesNotStartBackgroundJobServerIfNotConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(false);

        jobRunrBackgroundJobServerStarter.startup(new StartupEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsBackgroundJobServerIfConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);

        jobRunrBackgroundJobServerStarter.startup(new StartupEvent());

        verify(backgroundJobServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStopBackgroundJobServerIfNotConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(false);

        jobRunrBackgroundJobServerStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServerInstance, never()).get();
    }
}