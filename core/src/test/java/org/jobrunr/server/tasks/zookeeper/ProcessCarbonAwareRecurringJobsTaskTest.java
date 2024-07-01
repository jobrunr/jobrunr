package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.carbonaware.CarbonAwareJobManager;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.storage.RecurringJobsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.scheduling.carbonaware.CarbonAware.dailyBefore;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCarbonAwareRecurringJobsTaskTest extends AbstractJobZooKeeperTaskTest {

    ProcessRecurringJobsTask task;
    @Mock
    CarbonAwareJobManager carbonAwareJobManager;

    @BeforeEach
    void setUpTask() {
        task = new ProcessRecurringJobsTask(backgroundJobServer);
    }

    @Override
    protected CarbonAwareJobManager setUpCarbonAwareJobManager() {
        return carbonAwareJobManager;
    }

    @Test
    void taskMovesCarbonAwareRecurringJobsToNextState() {
        RecurringJob recurringJob = aDefaultRecurringJob().withId("rj1").withCronExpression("*/15 * * * * *").build();
        RecurringJob carbonAwareRecurringJob = aDefaultRecurringJob().withId("rj2").withCronExpression(dailyBefore(7)).build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob, carbonAwareRecurringJob)));

        runTask(task);

        verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        verify(carbonAwareJobManager, times(1)).moveToNextState(any());
        assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                .hasSize(3);
    }
}