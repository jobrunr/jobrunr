package org.jobrunr.server.dashboard;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.StorageProviderUtils;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.SevereJobRunrException.DiagnosticsAware;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardNotificationManagerTest {

    private UUID backgroundJobServerId;

    @Mock
    private StorageProvider storageProviderMock;

    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataToSaveArgumentCaptor;

    private DashboardNotificationManager dashboardNotificationManager;

    @BeforeEach
    void setUp() {
        backgroundJobServerId = UUID.randomUUID();
        dashboardNotificationManager = new DashboardNotificationManager(backgroundJobServerId, storageProviderMock);
    }

    @Test
    void handleSupportsSevereJobRunrExceptionAndShouldSaveJobRunrMetadataToStorageProvider() {
        dashboardNotificationManager.handle(new SevereJobRunrException("Severe exception occurred", new SomeException()));

        verify(storageProviderMock).saveMetadata(jobRunrMetadataToSaveArgumentCaptor.capture());

        assertThat(jobRunrMetadataToSaveArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServerId.toString())
                .valueContains("Runtime information")
                .valueContains("Timestamp")
                .valueContains("JobRunr Version")
                .valueContains("__StorageProvider__: " + storageProviderMock.getClass().getName())
                .valueContains("Background Job Servers")
                .valueContains("## Diagnostics from exception")
                .valueContains("### Title from inner exception")
                .valueContains("#### Title from inner exception")
                .valueContains("Line from inner exception");
    }

    @Test
    void notifyForCpuAllocationIrregularityShouldSaveJobRunrMetadataToStorageProvider() {
        dashboardNotificationManager.notify(new CpuAllocationIrregularityNotification(11));

        verify(storageProviderMock).saveMetadata(jobRunrMetadataToSaveArgumentCaptor.capture());

        assertThat(jobRunrMetadataToSaveArgumentCaptor.getValue())
                .hasName(CpuAllocationIrregularityNotification.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServerId.toString())
                .valueContains("11");
    }

    @Test
    void notifyForNewJobRunrVersionShouldSaveJobRunrMetadataToStorageProvider() {
        dashboardNotificationManager.notify(new NewJobRunrVersionNotification("4.0.0"));

        verify(storageProviderMock).saveMetadata(jobRunrMetadataToSaveArgumentCaptor.capture());

        assertThat(jobRunrMetadataToSaveArgumentCaptor.getValue())
                .hasName(NewJobRunrVersionNotification.class.getSimpleName())
                .hasOwner("cluster")
                .valueContains("4.0.0");
    }

    @Test
    void notificationsCanBeRetrievedReturnsNullIfNoneAvailable() {
        when(storageProviderMock.getMetadata(NewJobRunrVersionNotification.class.getSimpleName())).thenReturn(emptyList());

        final NewJobRunrVersionNotification dashboardNotification = dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class);

        assertThat(dashboardNotification).isNull();
    }

    @Test
    void notificationsCanBeRetrievedIfAvailable() {
        when(storageProviderMock.getMetadata(NewJobRunrVersionNotification.class.getSimpleName()))
                .thenReturn(asList(new JobRunrMetadata(NewJobRunrVersionNotification.class.getSimpleName(), StorageProviderUtils.Metadata.METADATA_OWNER_CLUSTER, "4.0.0")));

        final NewJobRunrVersionNotification dashboardNotification = dashboardNotificationManager.getDashboardNotification(NewJobRunrVersionNotification.class);

        assertThat(dashboardNotification.getLatestVersion()).isEqualTo("4.0.0");
    }

    private static class SomeException extends Exception implements DiagnosticsAware {

        @Override
        public DiagnosticsBuilder getDiagnosticsInfo() {
            return diagnostics()
                    .withTitle("Title from inner exception")
                    .withTitle("Subtitle from inner exception")
                    .withLine("Line from inner exception");
        }
    }
}