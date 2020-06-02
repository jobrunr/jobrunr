package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobZooKeeperTest {

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Captor
    private ArgumentCaptor<List<Job>> jobsToSaveArgumentCaptor;

    private BackgroundJobServerStatus backgroundJobServerStatus;
    private JobZooKeeper jobZooKeeper;
    private BackgroundJobTestFilter logAllStateChangesFilter;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        logAllStateChangesFilter = new BackgroundJobTestFilter();

        backgroundJobServerStatus = new BackgroundJobServerStatus(15, 10);
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobFilters(logAllStateChangesFilter));
        jobZooKeeper = new JobZooKeeper(backgroundJobServer);
        jobZooKeeper.setIsMaster(true);
    }

    @Test
    void jobZooKeeperDoesNothingIfItIsNotInitialized() {
        jobZooKeeper = new JobZooKeeper(backgroundJobServer);

        jobZooKeeper.run();

        verify(storageProvider, never()).getRecurringJobs();
        verify(storageProvider, never()).getScheduledJobs(any(), any());
        verify(storageProvider, never()).getJobs(any(), any(), any());
        verify(storageProvider, never()).deleteJobs(any(), any());
    }

    @Test
    void checkForRecurringJobs() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));

        jobZooKeeper.run();

        verify(backgroundJobServer).scheduleJob(recurringJob);
    }

    @Test
    void checkForRecurringJobsDoesNotScheduleSameJobIfItIsAlreadyScheduled() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));
        when(storageProvider.exists(recurringJob.getJobDetails(), SCHEDULED)).thenReturn(true);

        jobZooKeeper.run();

        verify(backgroundJobServer, never()).scheduleJob(recurringJob);
        verify(storageProvider, never()).exists(recurringJob.getJobDetails(), ENQUEUED);
        verify(storageProvider, never()).exists(recurringJob.getJobDetails(), PROCESSING);
    }

    @Test
    void checkForRecurringJobsDoesNotScheduleSameJobIfItIsAlreadyEnqueued() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));
        when(storageProvider.exists(recurringJob.getJobDetails(), SCHEDULED)).thenReturn(false);
        when(storageProvider.exists(recurringJob.getJobDetails(), ENQUEUED)).thenReturn(true);

        jobZooKeeper.run();

        verify(backgroundJobServer, never()).scheduleJob(recurringJob);
        verify(storageProvider, never()).exists(recurringJob.getJobDetails(), PROCESSING);
    }

    @Test
    void checkForRecurringJobsDoesNotScheduleSameJobIfItIsAlreadyProcessing() {
        RecurringJob recurringJob = aDefaultRecurringJob().build();

        when(storageProvider.getRecurringJobs()).thenReturn(List.of(recurringJob));
        when(storageProvider.exists(recurringJob.getJobDetails(), SCHEDULED)).thenReturn(false);
        when(storageProvider.exists(recurringJob.getJobDetails(), ENQUEUED)).thenReturn(false);
        when(storageProvider.exists(recurringJob.getJobDetails(), PROCESSING)).thenReturn(true);

        jobZooKeeper.run();

        verify(backgroundJobServer, never()).scheduleJob(recurringJob);
    }

    @Test
    void checkForEnqueuedJobsIfLessJobsThanWorkerPoolSizeTheyAreSubmittedNonBlocking() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(), any())).thenReturn(emptyList());
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), refEq(PageRequest.asc(0, 10)))).thenReturn(jobs);

        jobZooKeeper.run();

        verify(backgroundJobServer).processJob(enqueuedJob);
    }

    @Test
    void checkForEnqueuedJobsIfMoreJobsThanWorkerPoolSizeTheyAreSubmittedBlockingAsItResultsInLessConcurrentJobModifications() {
        final List<Job> enqueuedJobs = IntStream.range(0, 11)
                .mapToObj(x -> anEnqueuedJob().build())
                .collect(toList());

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(), any())).thenReturn(emptyList());
        lenient().when(storageProvider.getJobs(eq(ENQUEUED), refEq(PageRequest.asc(0, 10)))).thenReturn(enqueuedJobs);

        jobZooKeeper.run();

        verify(backgroundJobServer).processJobs(enqueuedJobs);
    }

    @Test
    void checkForEnqueuedJobsNotDoneIfRequestedStateIsStopped() {
        Job enqueuedJob = anEnqueuedJob().build();

        backgroundJobServerStatus.pause();

        jobZooKeeper.run();

        verify(storageProvider, never()).getJobs(eq(ENQUEUED), refEq(PageRequest.asc(0, 1)));
        verify(backgroundJobServer, never()).processJob(enqueuedJob);
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(Instant.class), refEq(PageRequest.asc(0, 1000))))
                .thenReturn(
                        singletonList(job),
                        emptyList()
                );

        jobZooKeeper.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SCHEDULED->ENQUEUED");
        assertThat(logAllStateChangesFilter.processingPassed).isFalse();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    @Test
    void checkForSucceededJobsThanCanGoToDeletedState() {
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), refEq(PageRequest.asc(0, 1000))))
                .thenReturn(
                        asList(aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build(), aSucceededJob().build()),
                        emptyList()
                );

        jobZooKeeper.run();

        verify(storageProvider).save(Mockito.any(List.class));
        verify(storageProvider).publishJobStatCounter(SUCCEEDED, 5);

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED", "SUCCEEDED->DELETED");
        assertThat(logAllStateChangesFilter.processingPassed).isFalse();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
    }

    @Test
    void checkForOrphanedJobs() {
        final Job orphanedJob = anEnqueuedJob().withState(new ProcessingState(backgroundJobServer.getId())).build();
        when(storageProvider.getJobs(eq(PROCESSING), any(Instant.class), any()))
                .thenReturn(
                        singletonList(orphanedJob),
                        emptyList()
                );

        jobZooKeeper.run();

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }

    @Test
    void checkForJobsThatCanBeDeleted() {
        when(storageProvider.deleteJobs(eq(DELETED), any())).thenReturn(5);

        jobZooKeeper.run();

        verify(storageProvider).deleteJobs(eq(DELETED), any());
    }

}