package org.jobrunr.server.tasks.zookeeper;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.RecurringJobsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessRecurringJobsTaskTest extends AbstractTaskTest {

    ProcessRecurringJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessRecurringJobsTask(backgroundJobServer);
    }

    @Test
    void testTask() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThatJobs(jobsToSaveArgumentCaptor.getValue())
                .hasSize(1)
                .allMatch(job -> job.hasState(SCHEDULED))
                .allMatch(job -> recurringJob.getId().equals(job.getRecurringJobId().orElse(null)));
    }

    @Test
    void recurringJobsAreCached() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true, false, true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task); // initial loading
        verify(storageProvider, times(1)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        runTask(task); // no updates to recurring jobs
        verify(storageProvider, times(2)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        runTask(task); // reload as recurring jobs updated
        verify(storageProvider, times(3)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(2)).getRecurringJobs();
    }

    @Test
    void taskDoesNotScheduleSameJobIfItIsAlreadyScheduledEnqueuedOrProcessed() {
        Instant now = now();
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        // FIRST RUN - No Jobs scheduled yet.
        try (MockedStatic<Instant> ignored = mockTime(now)) {
            when(storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(false);

            runTask(task);

            verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
            assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                    .hasSize(1)
                    .allMatch(job -> job.hasState(SCHEDULED))
                    .allMatch(job -> recurringJob.getId().equals(job.getRecurringJobId().orElse(null)));
        }


        // SECOND RUN - the 1 job scheduled in the first run is still active.
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval()))) {
            clearInvocations(storageProvider);
            when(storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(true);

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
        }

        // THIRD RUN - the 1 scheduled job is no longer active
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval().multipliedBy(2)))) {
            clearInvocations(storageProvider);
            when(storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(false);

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        }
    }

    @Test
    void taskSchedulesOneExtraJobAheadOfTime() {
        RecurringJob recurringJob = aDefaultRecurringJob().withIntervalExpression("PT24H").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);

        verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
        assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                .hasSize(1)
                .allMatch(j -> j.getRecurringJobId().orElse("").equals(recurringJob.getId()))
                .allMatch(j -> j.getState() == SCHEDULED);
    }

    @Test
    void ifJobIsScheduledAheadOfTimeTaskDoesNotCheckIfItIsAlreadyScheduledEnqueuedOrProcessed() {
        RecurringJob recurringJob = aDefaultRecurringJob().withIntervalExpression("PT24H").build();

        Instant lastScheduledAt = now().plus(10, HOURS);
        when(storageProvider.getRecurringJobsLatestScheduledRun()).thenReturn(Map.of(recurringJob.getId(), lastScheduledAt));
        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        ProcessRecurringJobsTask task = new ProcessRecurringJobsTask(backgroundJobServer);

        runTask(task);

        verify(storageProvider, never()).save(anyList());
        verify(storageProvider, never()).recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
    }

    @Test
    void taskSkipsAlreadyScheduledRecurringJobsOnStartup() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        Instant lastScheduledAt = now().plus(pollInterval());
        when(storageProvider.getRecurringJobsLatestScheduledRun()).thenReturn(Map.of(recurringJob.getId(), lastScheduledAt));
        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        ProcessRecurringJobsTask task = new ProcessRecurringJobsTask(backgroundJobServer);

        runTask(task);

        verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
        verify(storageProvider, never()).recurringJobExists(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
    }

    @Test
    void taskKeepsTrackOfRecurringJobRuns() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        when(storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(false);

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                    .hasSize(1)
                    .allMatch(j -> j.getRecurringJobId().orElse("").equals(recurringJob.getId()))
                    .allMatch(j -> j.getState() == SCHEDULED);
        }


        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR.plus(pollInterval()))) {
            clearInvocations(storageProvider);

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        }
    }

    @Test
    void taskDoesNotLogInfoMessageIfJobIsAlreadyScheduledEnqueuedOrProcessing() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression(Cron.minutely()).build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.minus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            clearInvocations(storageProvider);
        }

        when(storageProvider.recurringJobExists(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(true);
        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE)) {
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
            assertThat(logger).hasNoInfoMessageContaining("Run will be skipped as job is taking longer than given CronExpression or Interval");
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.plus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
            assertThat(logger).hasInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range 2022-12-14T08:36:00.500Z - 2022-12-14T08:36:05.500Z (PT5S) but it is already either AWAITING/SCHEDULED/ENQUEUED or PROCESSING and taking longer than given CronExpression or Interval. Run will be skipped.");
        }
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void taskSchedulesJobsThatWereMissedDuringStopTheWorldGC() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        runTask(task);
        sleep(10500);
        runTask(task);

        verify(storageProvider, times(2)).save(jobsToSaveArgumentCaptor.capture());
        assertThat(jobsToSaveArgumentCaptor.getValue())
                .hasSizeBetween(2, 3)
                .extracting(Job::getState)
                .containsOnly(SCHEDULED);
    }
}