package org.jobrunr.micronaut.autoconfigure;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrStarterTest {

    JobRunrStarter jobRunrStarter;

    @Mock
    JobRunrConfiguration configuration;

    @Mock
    JobRunrConfiguration.BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Mock
    JobRunrConfiguration.DashboardConfiguration dashboardConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    BackgroundJobServer backgroundJobServer;

    @Mock
    JobRunrDashboardWebServer dashboardWebServer;

    @BeforeEach
    void setUpJobRunrStarter() {
        when(configuration.getBackgroundJobServer()).thenReturn(backgroundJobServerConfiguration);
        when(configuration.getDashboard()).thenReturn(dashboardConfiguration);

        jobRunrStarter = new JobRunrStarter();
        setInternalState(jobRunrStarter, "configuration", configuration);
        setInternalState(jobRunrStarter, "storageProvider", storageProvider);
        setInternalState(jobRunrStarter, "backgroundJobServer", Optional.of(backgroundJobServer));
        setInternalState(jobRunrStarter, "dashboardWebServer", Optional.of(dashboardWebServer));
    }

    @Test
    void onStartOptionalsAreNotCalledToBootstrapIfNotConfigured() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(false);
        when(dashboardConfiguration.isEnabled()).thenReturn(false);

        jobRunrStarter.startup(null);

        verifyNoInteractions(backgroundJobServer);
        verifyNoInteractions(dashboardWebServer);
    }

    @Test
    void onStartOptionalsAreNotToBootstrapIfConfigured() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(true);
        when(dashboardConfiguration.isEnabled()).thenReturn(true);

        jobRunrStarter.startup(null);

        verify(backgroundJobServer).start();
        verify(dashboardWebServer).start();
    }

    @Test
    void onStopOptionalsAreNotCalledToBootstrapIfNotConfigured() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(false);
        when(dashboardConfiguration.isEnabled()).thenReturn(false);

        jobRunrStarter.shutdown(null);

        verifyNoInteractions(backgroundJobServer);
        verifyNoInteractions(dashboardWebServer);
    }

    @Test
    void onStopOptionalsAreNotToBootstrapIfConfigured() {
        when(backgroundJobServerConfiguration.isEnabled()).thenReturn(true);
        when(dashboardConfiguration.isEnabled()).thenReturn(true);

        jobRunrStarter.shutdown(null);

        verify(backgroundJobServer).stop();
        verify(dashboardWebServer).stop();
        verify(storageProvider).close();
    }
}