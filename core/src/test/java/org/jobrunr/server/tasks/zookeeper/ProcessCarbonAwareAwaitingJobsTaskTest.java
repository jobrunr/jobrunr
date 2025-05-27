package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStaticHolder;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractTaskTest {
    ProcessCarbonAwareAwaitingJobsTask task;

    CarbonAwareJobManager carbonAwareJobManager;

    @BeforeEach
    void setUp() {
        task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
        carbonAwareJobManager = backgroundJobServer.getCarbonAwareJobManager();
    }

    @Test
    void runTaskWithCarbonAwareDisabledDoesNothing() {
        when(carbonAwareJobManager.isDisabled()).thenReturn(true);

        task.runTask();

        verify(carbonAwareJobManager, times(0)).updateCarbonIntensityForecastIfNecessary();
    }

    @Test
    void taskCallsCarbonAwareJobManagerAndIfNoCarbonIntensityForecastAvailableSchedulesJobImmediatelyIfCarbonAwarePeriodIsBeforeTheRefreshTime() {
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T09:00:00Z"))) { // daily refresh time is at 19h if no data
            setNextRefreshTime(carbonAwareJobManager, now());

            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            runTask(task);

            Job updatedJob = storageProvider.getJobById(job.getId());
            assertThat(updatedJob).hasState(SCHEDULED);

            verify(carbonAwareJobManager).updateCarbonIntensityForecastIfNecessary();
            verify(carbonAwareJobManager).getAvailableForecastEndTime();
        }
    }

    @Test
    void taskCallsCarbonAwareJobManagerAndIfNoCarbonIntensityForecastAvailableKeepsAwaitingStateIfCarbonAwarePeriodIsAfterTheRefreshTime() {
        try (MockedStaticHolder ignored = mockTime(ZonedDateTime.parse("2025-05-27T17:00:00Z"))) { // daily refresh time is at 19h if no data
            setNextRefreshTime(carbonAwareJobManager, now());

            Job job = storageProvider.save(aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(6, HOURS))).build());

            runTask(task);

            Job updatedJob = storageProvider.getJobById(job.getId());
            assertThat(updatedJob).hasState(AWAITING);

            verify(carbonAwareJobManager).updateCarbonIntensityForecastIfNecessary();
            verify(carbonAwareJobManager).getAvailableForecastEndTime();
        }
    }

    @Test
    void taskCarbonAwaitingJobsToNextUsingCarbonAwareJobManager() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStaticHolder ignored = mockTime(currentTime)) {
            mockResponseWhenRequestingAreaCode("BE");

            List<Job> jobs = storageProvider.save(List.of(
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now(), now().plus(4, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(2, HOURS), now().plus(4, HOURS)), "schedule margin too small").build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(4, HOURS), now().plus(8, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(12, HOURS), now().plus(16, HOURS))).build(),
                    aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(36, HOURS), now().plus(48, HOURS)), "scheduled carbon-aware too far in the future").build()
            ));

            runTask(task);

            assertThatJob(jobs, 0)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().plus(1, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 1)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().plus(2, HOURS));
            assertThatJob(jobs, 2)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().plus(5, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 3)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().plus(13, HOURS).truncatedTo(HOURS));
            assertThatJob(jobs, 4)
                    .hasStates(AWAITING);
        }
    }

    private JobAssert assertThatJob(List<Job> jobs, int index) {
        return assertThat(storageProvider.getJobById(jobs.get(index).getId()));
    }

    private void setNextRefreshTime(CarbonAwareJobManager carbonAwareJobManager, Instant nextRefreshTime) {
        Whitebox.setInternalState(carbonAwareJobManager, "nextRefreshTime", nextRefreshTime);
    }
}
