package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.enterprise.inject.Instance;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobRunrStarterTest {

    JobRunrConfiguration configuration;

    JobRunrConfiguration.BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    JobRunrConfiguration.DashboardConfiguration dashboardConfiguration;

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
        configuration = new JobRunrConfiguration();
        backgroundJobServerConfiguration = new JobRunrConfiguration.BackgroundJobServerConfiguration();
        dashboardConfiguration = new JobRunrConfiguration.DashboardConfiguration();
        configuration.backgroundJobServer = backgroundJobServerConfiguration;
        configuration.dashboard = dashboardConfiguration;

        lenient().when(backgroundJobServerInstance.get()).thenReturn(backgroundJobServer);
        lenient().when(dashboardWebServerInstance.get()).thenReturn(dashboardWebServer);
        lenient().when(storageProviderInstance.get()).thenReturn(storageProvider);

        jobRunrStarter = new JobRunrStarter(configuration, backgroundJobServerInstance, dashboardWebServerInstance, storageProviderInstance);
    }

    @Test
    void jobRunrStarterDoesNotStartBackgroundJobServerIfNotConfigured() {
        backgroundJobServerConfiguration.enabled = false;

        jobRunrStarter.startup(new StartupEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsBackgroundJobServerIfConfigured() {
        backgroundJobServerConfiguration.enabled = true;

        jobRunrStarter.startup(new StartupEvent());

        verify(backgroundJobServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStartDashboardIfNotConfigured() {
        dashboardConfiguration.enabled = false;

        jobRunrStarter.startup(new StartupEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsDashboardIfConfigured() {
        dashboardConfiguration.enabled = true;

        jobRunrStarter.startup(new StartupEvent());

        verify(dashboardWebServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStopBackgroundJobServerIfNotConfigured() {
        backgroundJobServerConfiguration.enabled = false;

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsBackgroundJobServerIfConfigured() {
        backgroundJobServerConfiguration.enabled = true;

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServer).stop();
    }

    @Test
    void jobRunrStarterDoesNotStopsDashboardIfNotConfigured() {
        dashboardConfiguration.enabled = false;

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsDashboardIfConfigured() {
        dashboardConfiguration.enabled = true;

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServer).stop();
    }

    @Test
    void jobRunrStarterStopsStorageProvider() {
        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(storageProvider).close();
    }

}