package org.jobrunr.server.zookeeper.tasks;

import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.ProcessingState;
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
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.mockito.Mockito.*;

class UpdateJobsInProgressTaskTest extends AbstractZooKeeperTaskTest {

    UpdateJobsInProgressTask task;

    @BeforeEach
    void setUpTask() {
        task = new UpdateJobsInProgressTask(jobZooKeeper, backgroundJobServer);
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
        Assertions.assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
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
        verify(storageProvider).save(singletonList(job));
    }

    @Test
    void evenWhenNoWorkCanBeOnboardedJobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        // GIVEN
        final Job job = anEnqueuedJob().withId().build();
        startProcessingJob(job);

        // WHEN
        runTask(task, aDefaultBackgroundJobServerStatus().withWorkerSize(0).build());

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
        when(storageProvider.getJobById(job.getId())).thenReturn(aCopyOf(job).withDeletedState().build());
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
        when(storageProvider.getJobById(job.getId())).thenThrow(new JobNotFoundException(job.getId()));
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
        when(backgroundJobServer.getServerStatus()).thenReturn(aDefaultBackgroundJobServerStatus()
                        .withId()
                        .withName("my-host-name")
                        .build());

        job.startProcessingOn(backgroundJobServer);

        lenient().when(jobZooKeeper.getJobsInProgress()).thenReturn(Set.of(job));
        lenient().when(jobZooKeeper.getThreadProcessingJob(job)).thenReturn(threadMock);
        return threadMock;
    }
}