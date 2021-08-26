package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.NewJobRunrVersionNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CheckForNewJobRunrVersionTest {

    @Mock
    BackgroundJobServer backgroundJobServer;

    @Mock
    DashboardNotificationManager dashboardNotificationManager;

    @BeforeEach
    public void setUpBackgroundJobServer() {
        lenient().when(backgroundJobServer.getDashboardNotificationManager()).thenReturn(dashboardNotificationManager);
    }

    @Test
    void testCanGetLatestVersionFromGithubApi() throws IOException {
        final String latestVersion = CheckForNewJobRunrVersion.getLatestVersion();

        assertThat(latestVersion).matches("(\\d)+.(\\d)+.(\\d)+");
    }

    @Test
    void createsNotificationIfNewVersionIsFound() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getLatestVersion).thenReturn("4.0.0");
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("3.0.0");

            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager).notify(any(NewJobRunrVersionNotification.class));
        }
    }

    @Test
    void deletedNotificationIfLatestVersionIsInstalled() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getLatestVersion).thenReturn("4.0.0");
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("4.0.0");

            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager).deleteNotification(NewJobRunrVersionNotification.class);
        }
    }

}