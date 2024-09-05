package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration.BackgroundJobServerConfiguration;
import org.jobrunr.quarkus.autoconfigure.JobRunrBuildTimeConfiguration.DashboardConfiguration;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
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
class JobRunrStarterTest {

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Mock
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    DashboardConfiguration dashboardConfiguration;

    @Mock
    Instance<BackgroundJobServer> backgroundJobServerInstance;

    @Mock
    BackgroundJobServer backgroundJobServer;

    @Mock
    Instance<JobRunrDashboardWebServer> dashboardWebServerInstance;

    @Mock
    JobRunrDashboardWebServer dashboardWebServer;

    @Mock
    Instance<StorageProvider> storageProviderInstance;

    @Mock
    StorageProvider storageProvider;

    JobRunrStarter jobRunrStarter;

    @BeforeEach
    void setUpJobRunrMetricsStarter() {
        when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerConfiguration);
        when(jobRunrBuildTimeConfiguration.dashboard()).thenReturn(dashboardConfiguration);

        lenient().when(backgroundJobServerInstance.get()).thenReturn(backgroundJobServer);
        lenient().when(dashboardWebServerInstance.get()).thenReturn(dashboardWebServer);
        lenient().when(storageProviderInstance.get()).thenReturn(storageProvider);

        jobRunrStarter = new JobRunrStarter(jobRunrBuildTimeConfiguration, jobRunrRuntimeConfiguration, backgroundJobServerInstance, dashboardWebServerInstance, storageProviderInstance);
    }

    @Test
    void jobRunrStarterDoesNotStartBackgroundJobServerIfNotConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.startup(new StartupEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsBackgroundJobServerIfConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.startup(new StartupEvent());

        verify(backgroundJobServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStartDashboardIfNotConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.startup(new StartupEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsDashboardIfConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.startup(new StartupEvent());

        verify(dashboardWebServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStopBackgroundJobServerIfNotConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsBackgroundJobServerIfConfigured() {
        when(backgroundJobServerConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServer).stop();
    }

    @Test
    void jobRunrStarterDoesNotStopsDashboardIfNotConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsDashboardIfConfigured() {
        when(dashboardConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServer).stop();
    }

    @Test
    void jobRunrStarterStopsStorageProvider() {
        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(storageProvider).close();
    }

}
