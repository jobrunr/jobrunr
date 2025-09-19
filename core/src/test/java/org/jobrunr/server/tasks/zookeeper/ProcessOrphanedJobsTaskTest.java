package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Instant.now;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrphanedJobsTaskTest extends AbstractTaskTest {

    ProcessOrphanedJobsTask task;

    @BeforeEach
    void setUpTask() {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
        task = new ProcessOrphanedJobsTask(backgroundJobServer);
    }

    @Test
    void testTaskAndStateChangeFilters() {
        final Job orphanedJob = anEnqueuedJob().withProcessingState(now().minusSeconds(120)).build();
        saveJobsInStorageProvider(orphanedJob);

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        var job = jobsToSaveArgumentCaptor.getAllValues().get(0).get(0);
        assertThat(job).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
        assertThat(logAllStateChangesFilter.getStateChanges(orphanedJob)).containsExactly("PROCESSING->FAILED", "FAILED->SCHEDULED");
        assertThat(logAllStateChangesFilter.onProcessingFailedIsCalled(orphanedJob)).isTrue();

        var failedState = job.getLastJobStateOfType(FailedState.class).orElseThrow();
        assertThat(failedState.getStackTrace()).isNotEmpty();
        assertThat(failedState.getStackTrace()).isEqualTo("org.jobrunr.jobs.exceptions.IllegalJobThreadStateException: Job was too long in PROCESSING state without being updated.\n");
    }

}