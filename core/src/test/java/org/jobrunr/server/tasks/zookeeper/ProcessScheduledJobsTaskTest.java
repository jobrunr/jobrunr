package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.navigation.AmountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessScheduledJobsTaskTest extends AbstractTaskTest {

    ProcessScheduledJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessScheduledJobsTask(backgroundJobServer);
    }

    @Test
    void testTask() {
        final Job scheduledJob = aScheduledJob().build();

        when(storageProvider.getScheduledJobs(any(), any(AmountRequest.class))).thenReturn(singletonList(scheduledJob), emptyJobList());

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue().get(0)).hasStates(SCHEDULED, ENQUEUED);
    }
}