package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.DatetimeMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCarbonAwaitingJob;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessCarbonAwareAwaitingJobsTaskTest extends AbstractTaskTest {
    private ProcessCarbonAwareAwaitingJobsTask task;
    private CarbonAwareJobManager carbonAwareJobManager;

    @Captor
    ArgumentCaptor<Instant> instantArgumentCaptor;

    @BeforeEach
    void setUp() {
        task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
        carbonAwareJobManager = backgroundJobServer.getCarbonAwareJobManager();
    }

    @Test
    void taskCallsCarbonAwareJobManager() {
        Job job = aCarbonAwaitingJob().build();

        when(storageProvider.getCarbonAwareJobList(any(), any())).thenReturn(List.of(job));

        runTask(task);

        verify(carbonAwareJobManager).moveToNextState(job);
        verify(carbonAwareJobManager).updateCarbonIntensityForecastIfNecessary();
        verify(carbonAwareJobManager).getAvailableForecastEndTime();
    }

    @Test
    void taskCarbonAwaitingJobsToNextUsingCarbonAwareJobManager() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStatic<Instant> ignored1 = mockTime(currentTime.toInstant());
             MockedStatic<ZonedDateTime> ignored2 = DatetimeMocker.mockZonedDateTime(currentTime, ZoneId.systemDefault())) {
            mockResponseWhenRequestingAreaCode("BE");

            runTask(task);

            verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
            assertThat(instantArgumentCaptor.getValue()).isEqualTo(toEndOfNextDay(currentTime));

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(Instant.now(), Instant.now().plus(4, HOURS))).build();
            Job job2 = aJob().withState(new CarbonAwareAwaitingState(Instant.now().plus(2, HOURS), Instant.now(), Instant.now().plus(4, HOURS))).build();
            Job job3 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(Instant.now().plus(4, HOURS), Instant.now().plus(8, HOURS))).build();
            Job job4 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(Instant.now().plus(12, HOURS), Instant.now().plus(16, HOURS))).build();

            when(storageProvider.getCarbonAwareJobList(any(), any())).thenReturn(List.of(job1, job2, job3, job4));
            runTask(task);

            assertThat(job1)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(Instant.now().plus(1, HOURS).truncatedTo(HOURS));
            assertThat(job2)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(Instant.now().plus(1, HOURS).truncatedTo(HOURS));
            assertThat(job3)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(Instant.now().plus(5, HOURS).truncatedTo(HOURS));
            assertThat(job4)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(Instant.now().plus(13, HOURS).truncatedTo(HOURS));
        }
    }

    @Test
    void jobsAreScheduledEvenIfCarbonAwareSchedulingIsNotEnabled() {
        Instant now = now();
        CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(usingStandardCarbonAwareConfiguration().andCarbonAwareSchedulingEnabled(false), getJsonMapper());
        doReturn(carbonAwareJobManager).when(backgroundJobServer).getCarbonAwareJobManager();
        ProcessCarbonAwareAwaitingJobsTask task = new ProcessCarbonAwareAwaitingJobsTask(backgroundJobServer);
        Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now, now.plus(4, HOURS))).build();
        Job job2 = aJob().withState(new CarbonAwareAwaitingState(now.plus(2, HOURS), now, now.plus(4, HOURS))).build();

        when(storageProvider.getCarbonAwareJobList(any(), any())).thenReturn(List.of(job1, job2));
        runTask(task);

        verify(storageProvider).getCarbonAwareJobList(instantArgumentCaptor.capture(), any());
        assertThat(instantArgumentCaptor.getValue()).isCloseTo(now.plus(365, DAYS), within(1, SECONDS));
        assertThat(job1)
                .hasStates(AWAITING, SCHEDULED)
                .isScheduledAt(now, "Carbon aware scheduling is not enabled. Job will be scheduled at pre-defined preferred instant.");
        assertThat(job2)
                .hasStates(AWAITING, SCHEDULED)
                .isScheduledAt(now.plus(2, HOURS), "Carbon aware scheduling is not enabled. Job will be scheduled at pre-defined preferred instant.");
    }
}
