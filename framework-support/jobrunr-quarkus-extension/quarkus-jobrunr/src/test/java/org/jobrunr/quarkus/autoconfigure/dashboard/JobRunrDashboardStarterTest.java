package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration.DashboardConfiguration;
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
class JobRunrDashboardStarterTest {
    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Mock
    DashboardConfiguration dashboardConfiguration;

    @Mock
    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;

    @Mock
    JobRunrDashboardWebServer dashboardWebServer;

    JobRunrDashboardStarter jobRunrDashboardStarter;

    @BeforeEach
    void setup() {
        when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardConfiguration);

        lenient().when(dashboardWebServerInstance.get()).thenReturn(dashboardWebServer);

        jobRunrDashboardStarter = new JobRunrDashboardStarter(jobRunrRuntimeConfiguration, dashboardWebServerInstance);
    }

    @Test
    void jobRunrStarterDoesNotStartDashboardIfNotConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(false);

        jobRunrDashboardStarter.startup(new StartupEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsDashboardIfConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(true);

        jobRunrDashboardStarter.startup(new StartupEvent());

        verify(dashboardWebServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStopsDashboardIfNotConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(false);

        jobRunrDashboardStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsDashboardIfConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(true);

        jobRunrDashboardStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServer).stop();
    }
}