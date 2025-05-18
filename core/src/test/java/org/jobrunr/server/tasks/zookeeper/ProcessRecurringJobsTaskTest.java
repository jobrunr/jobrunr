package org.jobrunr.server.tasks.zookeeper;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.RecurringJobsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_ON_THE_MINUTE;
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
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        Instant scheduledAt;

        // FIRST RUN - No Jobs scheduled yet.
        try (MockedStatic<Instant> ignored = mockTime(now)) {
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of());

            runTask(task);

            verify(storageProvider).save(jobsToSaveArgumentCaptor.capture());
            List<Job> scheduledJobs = jobsToSaveArgumentCaptor.getAllValues().get(0);
            assertThatJobs(scheduledJobs)
                    .hasSize(1)
                    .allMatch(job -> job.hasState(SCHEDULED))
                    .allMatch(job -> recurringJob.getId().equals(job.getRecurringJobId().orElse(null)));

            scheduledAt = ((ScheduledState) scheduledJobs.get(0).getJobState()).getScheduledAt();
        }


        // SECOND RUN - the 1 job scheduled in the first run is still active.
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval()))) {
            clearInvocations(storageProvider);
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of(scheduledAt));

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
        }

        // THIRD RUN - the 1 scheduled job is no longer active
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval().multipliedBy(2)))) {
            clearInvocations(storageProvider);
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of());

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
    void taskSkipsAlreadyScheduledRecurringJobsOnStartup() {
        Instant now = now();
        RecurringJob recurringJob = aDefaultRecurringJob().withIntervalExpression("PT1H").build();

        Instant lastScheduledAt = now().plusSeconds(6);
        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        ProcessRecurringJobsTask task = new ProcessRecurringJobsTask(backgroundJobServer);

        // FIRST RUN - 1 job is already scheduled
        try (MockedStatic<Instant> ignored = mockTime(now)) {
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of(lastScheduledAt));

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
        }

        // SECOND RUN - the job is still scheduled but will be moved to enqueued.
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval()))) {
            clearInvocations(storageProvider);
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of(lastScheduledAt));

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
            verify(storageProvider, never()).getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING);
            assertThat(logger).hasNoInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range");
        }

        // THIRD RUN - the 1 scheduled job is no longer active
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval().multipliedBy(2)))) {
            clearInvocations(storageProvider);
            when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of());

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        }
    }

    @Test
    void taskKeepsTrackOfRecurringJobRuns() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of());

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
    void taskDoesNotLogInfoMessageIfJobIsScheduledButYetToPassTheNextScheduledInstant() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression(Cron.minutely()).build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.minus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            clearInvocations(storageProvider);
        }

        when(storageProvider.getRecurringJobScheduledInstants(recurringJob.getId(), SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(List.of(FIXED_INSTANT_RIGHT_ON_THE_MINUTE));
        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE)) {
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
            assertThat(logger).hasNoInfoMessageContaining("Run will be skipped as job is taking longer than given CronExpression or Interval");
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.plus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
            assertThat(logger).hasNoInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range 2022-12-14T08:36:00.500Z - 2022-12-14T08:36:05.500Z (PT5S) but it is already SCHEDULED, ENQUEUED or PROCESSING. Run will be skipped as job is taking longer than given CronExpression or Interval.");
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.plusSeconds(60))) { // time skip
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
            assertThat(logger).hasInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range 2022-12-14T08:36:55.500Z - 2022-12-14T08:37:00.500Z (PT5S) but it is already SCHEDULED, ENQUEUED or PROCESSING.");
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
