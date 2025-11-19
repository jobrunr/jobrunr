package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.tasks.Task;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static java.util.Collections.emptyList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class JobHandlerTest {

    BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();

    @Mock
    StorageProvider storageProvider;
    @Captor
    ArgumentCaptor<JobRunrMetadata> jobRunrMetadataArgumentCaptor;

    ListAppender<ILoggingEvent> logger;

    @BeforeEach
    void setUpJobHandler() {
        UUID backgroundJobServerId = backgroundJobServer.getId();
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getDashboardNotificationManager()).thenReturn(new DashboardNotificationManager(backgroundJobServerId, storageProvider));
    }

    @Test
    void jobHandlerDoesNothingIfItIsNotInitialized() {
        Task mockedTask = mock(Task.class);
        JobHandler jobHandler = createJobHandlerWithTask(mockedTask);

        when(backgroundJobServer.isNotReadyToProcessJobs()).thenReturn(true);

        jobHandler.run();

        verifyNoInteractions(mockedTask);
    }

    @Test
    void severeJobRunrExceptionsAreLoggedToStorageProvider() {
        Task mockedTask = mockTaskThatThrows(new SevereJobRunrException("Could not resolve ConcurrentJobModificationException", new UnresolvableConcurrentJobModificationException(emptyList(), null)));
        JobHandler jobHandler = createJobHandlerWithTask(mockedTask);

        jobHandler.run();

        verify(storageProvider).saveMetadata(jobRunrMetadataArgumentCaptor.capture());

        assertThat(jobRunrMetadataArgumentCaptor.getValue())
                .hasName(SevereJobRunrException.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId())
                .valueContains("## Runtime information");
    }

    @Test
    void jobHandlerStopsBackgroundJobServerIfTooManyExceptions() {
        Task mockedTask = mockTaskThatThrows(new SevereJobRunrException("Could not resolve ConcurrentJobModificationException", new UnresolvableConcurrentJobModificationException(emptyList(), null)));
        JobHandler jobHandler = createJobHandlerWithTask(mockedTask);

        for (int i = 0; i <= 5; i++) {
            jobHandler.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many processing exceptions. Shutting down.");
    }

    @Test
    void jobHandlersStopsBackgroundJobServerAndLogsStorageProviderExceptionIfTooManyStorageExceptions() {
        Task mockedTask = mockTaskThatThrows(new StorageException("a storage exception"));
        JobHandler jobHandler = createJobHandlerWithTask(mockedTask);

        for (int i = 0; i <= 5; i++) {
            jobHandler.run();
        }

        verify(backgroundJobServer).stop();
        assertThat(logger).hasErrorMessage("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/");
    }

    private Task mockTaskThatThrows(Exception e) {
        Task mockedTask = mock(Task.class);
        doThrow(e)
                .when(mockedTask).run(Mockito.any());
        return mockedTask;
    }

    private JobHandler createJobHandlerWithTask(Task task) {
        JobHandler jobHandler = new JobHandler(backgroundJobServer, task) {
        };
        logger = LoggerAssert.initFor(jobHandler);
        return jobHandler;
    }
}