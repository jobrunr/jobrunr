package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.util.Collections.singletonList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessOrphanedJobsTaskTest extends AbstractZooKeeperTaskTest {

    ProcessOrphanedJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessOrphanedJobsTask(jobZooKeeper, backgroundJobServer);
    }

    @Test
    void testTask() {
        final Job orphanedJob = aJobInProgress().build();
        when(storageProvider.getJobs(eq(PROCESSING), any(Instant.class), any()))
                .thenReturn(
                        singletonList(orphanedJob),
                        emptyJobList()
                );

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }
}