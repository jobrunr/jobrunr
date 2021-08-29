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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void onFirstRunDoesNotContactGithubApiToNotSpamGithubApiFromDeveloperPC_NoPreviousNotificationStored() {
        final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
        when(dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class)).thenReturn(null);

        checkForNewJobRunrVersion.run();

        verify(dashboardNotificationManager, never()).notify(any(NewJobRunrVersionNotification.class));
    }

    @Test
    void onFirstRunDoesNotContactGithubApiToNotSpamGithubApiFromDeveloperPC_PreviousNotificationStoredAndLocalVersionNotUpdated() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("3.0.0");

            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
            when(dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class)).thenReturn(new NewJobRunrVersionNotification("4.0.0"));

            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager, never()).notify(any(NewJobRunrVersionNotification.class));
            verify(dashboardNotificationManager, never()).deleteNotification(NewJobRunrVersionNotification.class);
        }
    }

    @Test
    void onFirstRunDoesNotContactGithubApiToNotSpamGithubApiFromDeveloperPC_PreviousNotificationStoredAndLocalVersionUpdated() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("4.0.0");

            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
            when(dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class)).thenReturn(new NewJobRunrVersionNotification("4.0.0"));

            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager, never()).notify(any(NewJobRunrVersionNotification.class));
            verify(dashboardNotificationManager).deleteNotification(NewJobRunrVersionNotification.class);
        }
    }

    @Test
    void onSubsequentRunsCreatesNotificationIfNewVersionIsFound() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getLatestVersion).thenReturn("4.0.0");
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("3.0.0");

            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);

            checkForNewJobRunrVersion.run();
            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager).notify(any(NewJobRunrVersionNotification.class));
        }
    }

    @Test
    void onSubsequentRunsCreatesNotificationIfNewVersionIsFoundAlsoSupportsBetaVersions() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getLatestVersion).thenReturn("4.0.0");
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("4.0.0-RC1");

            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);

            checkForNewJobRunrVersion.run();
            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager).notify(any(NewJobRunrVersionNotification.class));
        }
    }

    @Test
    void onSubsequentRunsDeletesNotificationIfLatestVersionIsInstalled() {
        try (MockedStatic<CheckForNewJobRunrVersion> checkForNewJobRunrVersionStaticMock = Mockito.mockStatic(CheckForNewJobRunrVersion.class)) {
            final CheckForNewJobRunrVersion checkForNewJobRunrVersion = new CheckForNewJobRunrVersion(backgroundJobServer);
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getLatestVersion).thenReturn("4.0.0");
            checkForNewJobRunrVersionStaticMock.when(CheckForNewJobRunrVersion::getActualVersion).thenReturn("4.0.0");

            checkForNewJobRunrVersion.run();
            checkForNewJobRunrVersion.run();

            verify(dashboardNotificationManager).deleteNotification(NewJobRunrVersionNotification.class);
        }
    }

}