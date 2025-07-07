package org.jobrunr.server.tasks.steward;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.Collections.singletonList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateJobsInProgressTaskTest extends AbstractTaskTest {

    UpdateJobsInProgressTask task;

    @BeforeEach
    void setUpTask() {
        task = new UpdateJobsInProgressTask(backgroundJobServer);
    }

    @Test
    void jobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        startProcessingJob(job);

        // WHEN
        runTask(task);

        // THEN
        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void noExceptionIsThrownIfAJobHasSucceededWhileUpdateProcessingIsCalled() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        startProcessingJob(job);

        // WHEN
        job.succeeded();
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        verify(storageProvider, never()).save(anyList());
    }

    @Test
    void evenWhenNoWorkCanBeOnboardedJobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        startProcessingJob(job);

        // WHEN
        runTask(task);

        // THEN
        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        Assertions.assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void jobsThatAreBeingProcessedButHaveBeenDeletedViaDashboardWillBeInterrupted() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        doThrow(new ConcurrentJobModificationException(job)).when(storageProvider).save(singletonList(job));
        doReturn(aCopyOf(job).withDeletedState().build()).when(storageProvider).getJobById(job.getId());
        final Thread threadMock = startProcessingJobAndReturnThread(job);

        // WHEN
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        assertThat(job).hasState(DELETED);
        verify(storageProvider).save(singletonList(job));
        verify(threadMock).interrupt();
    }

    @Test
    void jobsThatAreBeingProcessedButArePermanentlyDeletedViaAPIWillBeInterrupted() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        doThrow(new ConcurrentJobModificationException(job)).when(storageProvider).save(singletonList(job));
        doThrow(new JobNotFoundException(job.getId())).when(storageProvider).getJobById(job.getId());
        final Thread threadMock = startProcessingJobAndReturnThread(job);

        // WHEN
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        assertThat(job).hasState(DELETED);
        verify(storageProvider).save(singletonList(job));
        verify(threadMock).interrupt();
    }

    void startProcessingJob(Job job) {
        startProcessingJobAndReturnThread(job);
    }

    Thread startProcessingJobAndReturnThread(Job job) {
        final Thread threadMock = mock(Thread.class);

        job.startProcessingOn(backgroundJobServer);

        lenient().when(jobSteward.getJobsInProgress()).thenReturn(Set.of(job));
        lenient().when(jobSteward.getThreadProcessingJob(job)).thenReturn(threadMock);
        return threadMock;
    }
}