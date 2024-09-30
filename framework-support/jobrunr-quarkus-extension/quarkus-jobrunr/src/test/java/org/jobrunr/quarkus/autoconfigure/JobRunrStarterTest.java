package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrStarterTest {

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration backgroundJobServerBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.DashboardConfiguration dashboardBuildTimeConfiguration;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.BackgroundJobServerConfiguration backgroundJobServerRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.DashboardConfiguration dashboardRuntimeConfiguration;


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
    void setup() {
        lenient().when(jobRunrBuildTimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerBuildTimeConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.dashboard()).thenReturn(dashboardBuildTimeConfiguration);

        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerRuntimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardRuntimeConfiguration);

        lenient().when(backgroundJobServerInstance.get()).thenReturn(backgroundJobServer);
        lenient().when(backgroundJobServerInstance.isResolvable()).thenReturn(true);
        lenient().when(dashboardWebServerInstance.get()).thenReturn(dashboardWebServer);
        lenient().when(dashboardWebServerInstance.isResolvable()).thenReturn(true);
        lenient().when(storageProviderInstance.get()).thenReturn(storageProvider);

        jobRunrStarter = new JobRunrStarter(jobRunrBuildTimeConfiguration, jobRunrRuntimeConfiguration, backgroundJobServerInstance, dashboardWebServerInstance, storageProviderInstance);
    }

    @Test
    void jobRunrStarterStartsBackgroundJobServerIfIncludedAndEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(new StartupEvent())).doesNotThrowAnyException();
        verify(backgroundJobServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStartTheBackgroundJobServerIfIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(true);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(false);

        jobRunrStarter.startup(new StartupEvent());
        verify(backgroundJobServerInstance, never()).get();
        verify(backgroundJobServer, never()).start();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheBackgroundJobServerIsNotIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(false);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(new StartupEvent())).doesNotThrowAnyException();
        verify(backgroundJobServer, never()).start();
    }

    @Test
    void jobRunrStarterDoesThrowAnExceptionOnStartupIfTheBackgroundJobServerIsNotIncludedButEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(false);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(new StartupEvent())).
                isInstanceOf(IllegalStateException.class)
                .hasMessage("The BackgroundJobServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the BackgroundJobServer.");
    }

    @Test
    void jobRunrStarterStopsBackgroundJobServerIfEnabled() {
        when(backgroundJobServerRuntimeConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServer).stop();
    }

    @Test
    void jobRunrStarterDoesNotStopBackgroundJobServerIfNotEnabled() {
        when(backgroundJobServerRuntimeConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(backgroundJobServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStartsDashboardIfIncludedAndEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
        verify(dashboardWebServer).start();
    }

    @Test
    void jobRunrStarterDoesNotStartTheDashboardIfIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(false);

        jobRunrStarter.startup(new StartupEvent());
        verify(dashboardWebServerInstance, never()).get();
        verify(dashboardWebServer, never()).start();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheDashboardIsNotIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(false);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(new StartupEvent())).doesNotThrowAnyException();
        verify(dashboardWebServer, never()).start();
    }

    @Test
    void jobRunrStarterDoesThrowAnExceptionOnStartupIfTheDashboardIsNotIncludedButEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(false);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(new StartupEvent()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The JobRunrDashboardWebServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the JobRunrDashboardWebServer.");
    }

    @Test
    void jobRunrStarterStopsDashboardIfEnabled() {
        when(dashboardRuntimeConfiguration.enabled()).thenReturn(true);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServer).stop();
    }

    @Test
    void jobRunrStarterDoesNotStopDashboardIfNotEnabled() {
        when(dashboardRuntimeConfiguration.enabled()).thenReturn(false);

        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(dashboardWebServerInstance, never()).get();
    }

    @Test
    void jobRunrStarterStopsStorageProvider() {
        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(storageProvider).close();
    }

}