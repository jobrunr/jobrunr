package org.jobrunr.server.tasks.other;

import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.carbonaware.CarbonAwareJobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCarbonAwareAwaitingJobsAbstractTaskTest extends AbstractTaskTest {
    private ProcessCarbonAwareAwaitingJobsTask task;

    @BeforeEach
    void setUp() {
        task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
    }

    @Test
    public void testUpdateAwaitingJobs_whenHasRunToday_shouldSkip() {
        CarbonAwareJobManager carbonAwareJobManager = CarbonAwareJobManager.getInstance();
        runTask(task);
        when(storageProvider.getMetadata("process_carbon_aware_jobs_last_run", "cluster")).thenReturn(new JobRunrMetadata("process_carbon_aware_jobs_last_run", "cluster", Instant.now().toString()));
        runTask(task);
        verify(carbonAwareJobManager, times(1)).updateDayAheadEnergyPrices();
    }
}
