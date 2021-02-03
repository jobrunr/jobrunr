package org.jobrunr.server;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.diagnostics.DiagnosticsBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.SevereJobRunrException.DiagnosticsAware;
import static org.jobrunr.utils.diagnostics.DiagnosticsBuilder.diagnostics;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SevereExceptionManagerTest {

    @Mock
    private BackgroundJobServer backgroundJobServerMock;

    @Mock
    private StorageProvider storageProviderMock;

    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataToSaveArgumentCaptor;

    private SevereExceptionManager severeExceptionManager;

    @BeforeEach
    void setUp() {
        when(backgroundJobServerMock.getId()).thenReturn(UUID.randomUUID());
        when(backgroundJobServerMock.getStorageProvider()).thenReturn(storageProviderMock);

        severeExceptionManager = new SevereExceptionManager(backgroundJobServerMock);
    }

    @Test
    void handleShouldSaveJobRunrMetadataToStorageProvider() {
        severeExceptionManager.handle(new SevereJobRunrException("Severe exception occurred", new SomeException()));

        verify(storageProviderMock).saveMetadata(jobRunrMetadataToSaveArgumentCaptor.capture());

        assertThat(jobRunrMetadataToSaveArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServerMock.getId().toString())
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