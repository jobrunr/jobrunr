package org.jobrunr.micronaut.autoconfigure;

import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JobRunrStarterTest {

    @Mock
    StorageProvider storageProvider;

    @Mock
    BackgroundJobServer backgroundJobServer;

    @Mock
    JobRunrDashboardWebServer dashboardWebServer;

    @Test
    void onStartOptionalsAreNotCalledToBootstrapIfNotConfigured() {
        var jobRunrStarter = new JobRunrStarter(storageProvider, Optional.empty(), Optional.empty());

        jobRunrStarter.startup(null);

        verifyNoInteractions(backgroundJobServer);
        verifyNoInteractions(dashboardWebServer);
        verifyNoInteractions(storageProvider);
    }

    @Test
    void onStartOptionalsAreCalledToBootstrapIfConfigured() {
        var jobRunrStarter = new JobRunrStarter(storageProvider, Optional.of(backgroundJobServer), Optional.of(dashboardWebServer));

        jobRunrStarter.startup(null);

        verify(backgroundJobServer).start();
        verify(dashboardWebServer).start();
        verifyNoInteractions(storageProvider);
    }

    @Test
    void onStopOptionalsAreNotCalledToBootstrapIfNotConfigured() {
        var jobRunrStarter = new JobRunrStarter(storageProvider, Optional.empty(), Optional.empty());

        jobRunrStarter.shutdown(null);

        verifyNoInteractions(backgroundJobServer);
        verifyNoInteractions(dashboardWebServer);
        verify(storageProvider).close();
    }

    @Test
    void onStopOptionalsAreNotToBootstrapIfConfigured() {
        var jobRunrStarter = new JobRunrStarter(storageProvider, Optional.of(backgroundJobServer), Optional.of(dashboardWebServer));

        jobRunrStarter.shutdown(null);

        verify(backgroundJobServer).stop();
        verify(dashboardWebServer).stop();
        verify(storageProvider).close();
    }
}