package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.Mockito.*;

class ProcessScheduledJobsTaskTest extends AbstractZooKeeperTaskTest {

    ProcessScheduledJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessScheduledJobsTask(jobZooKeeper, backgroundJobServer);
    }

    @Test
    void testTask() {
        final Job scheduledJob = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(), any())).thenReturn(asList(scheduledJob), emptyJobList());

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(SCHEDULED, ENQUEUED);
    }
}