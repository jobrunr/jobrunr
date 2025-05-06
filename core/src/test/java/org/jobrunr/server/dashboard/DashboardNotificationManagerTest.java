package org.jobrunr.server.dashboard;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.SevereJobRunrException.DiagnosticsAware;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
    void noExceptionIsThrownForConcurrentSlowRunNotification() {
        doThrow(new StorageException("a storage exception"))
                .when(storageProviderMock).saveMetadata(any(JobRunrMetadata.class));

        assertThatCode(() -> dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(1, 5, Instant.now(), 6)))
                .doesNotThrowAnyException();
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