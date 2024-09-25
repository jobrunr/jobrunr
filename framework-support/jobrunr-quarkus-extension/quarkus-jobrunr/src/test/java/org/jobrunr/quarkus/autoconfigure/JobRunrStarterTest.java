package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.runtime.ShutdownEvent;
import jakarta.enterprise.inject.Instance;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.lenient;
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

        lenient().when(storageProviderInstance.get()).thenReturn(storageProvider);

        jobRunrStarter = new JobRunrStarter(jobRunrBuildTimeConfiguration, jobRunrRuntimeConfiguration, storageProviderInstance);
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheBackgroundJobServerIsIncludedAndEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheBackgroundJobServerIsIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(false);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheBackgroundJobServerIsNotIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(false);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheBackgroundJobServerIsNotIncludedButEnabled() {
        when(jobRunrBuildTimeConfiguration.backgroundJobServer().included()).thenReturn(false);
        when(jobRunrRuntimeConfiguration.backgroundJobServer().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(null)).
                isInstanceOf(IllegalStateException.class)
                .hasMessage("The BackgroundJobServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the BackgroundJobServer.");
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheDashboardIsIncludedAndEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheDashboardIsIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(true);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheDashboardIsNotIncludedAndNotEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(false);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(false);

        assertThatCode(() -> jobRunrStarter.startup(null)).doesNotThrowAnyException();
    }

    @Test
    void jobRunrStarterDoesNotThrowAnExceptionOnStartupIfTheDashboardIsNotIncludedButEnabled() {
        when(jobRunrBuildTimeConfiguration.dashboard().included()).thenReturn(false);
        lenient().when(jobRunrRuntimeConfiguration.dashboard().enabled()).thenReturn(true);

        assertThatCode(() -> jobRunrStarter.startup(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The JobRunrDashboardWebServer cannot be enabled, its resources were not included at build time. Please rebuild your project to include the required resources or disable the JobRunrDashboardWebServer.");
    }

    @Test
    void jobRunrStarterStopsStorageProvider() {
        jobRunrStarter.shutdown(new ShutdownEvent());

        verify(storageProvider).close();
    }

}