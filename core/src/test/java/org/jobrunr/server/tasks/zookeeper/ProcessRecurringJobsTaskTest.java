package org.jobrunr.server.tasks.zookeeper;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.assertj.core.api.IdListAssert;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobAssert;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.Paging;
import org.jobrunr.storage.RecurringJobsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.MockedStaticHolder;
import org.mockito.verification.VerificationMode;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Collections.emptyList;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.InstantMocker.FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_ON_THE_MINUTE;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
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

        storageProvider.saveRecurringJob(recurringJob);

        runTask(task);

        // THEN
        assertThatSavedScheduledJobs()
                .singleElement()
                .hasState(SCHEDULED)
                .hasRecurringJobId(recurringJob.getId());
    }

    @Test
    void recurringJobsAreCached() {
        RecurringJob recurringJob1 = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();
        storageProvider.saveRecurringJob(recurringJob1);

        runTask(task); // initial loading
        verify(storageProvider, times(1)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        runTask(task); // no updates to recurring jobs
        verify(storageProvider, times(2)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(1)).getRecurringJobs();

        RecurringJob recurringJob2 = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();
        storageProvider.saveRecurringJob(recurringJob2);

        runTask(task); // reload as recurring jobs updated
        verify(storageProvider, times(3)).recurringJobsUpdated(anyLong());
        verify(storageProvider, times(2)).getRecurringJobs();
    }

    @Test
    void taskDoesNotScheduleSameJobIfItIsAlreadyScheduledEnqueuedOrProcessed() {
        Instant now = Instant.parse("2025-05-27T11:12:52Z");
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        storageProvider.saveRecurringJob(recurringJob);

        // FIRST RUN - No Jobs scheduled yet.
        Instant expectedScheduledAt = Instant.parse("2025-05-27T11:12:55Z");
        try (MockedStatic<Instant> ignored = mockTime(now)) { // at second 52
            runTask(task);

            // THEN
            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasRecurringJobId(recurringJob.getId())
                    .hasScheduledAt(expectedScheduledAt);
        }

        // SECOND RUN - the 1 job scheduled in the first run is still active.
        clearStorageProviderInvocationsAndCaptors();
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval()))) { // at second 57
            runTask(task);

            verify(storageProvider, never()).save(jobsToSaveArgumentCaptor.capture());
        }

        // THIRD RUN - the 1 scheduled job is no longer active
        clearStorageProviderInvocationsAndCaptors();
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval().multipliedBy(2)))) {
            runScheduledJobInstanceOfRecurringJob(recurringJob.getId());

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        }
    }

    @Test
    void testTaskSchedulesOneJobExtraAheadOfTimeForRecurringJobsUsingCronExpressionThatRunLessThanOncePerMinute() {
        ZonedDateTime zonedDateTime = FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR.atZone(ZoneId.systemDefault());
        RecurringJob recurringJob;
        try (MockedStaticHolder ignored = mockTime(zonedDateTime)) {
            // GIVEN
            recurringJob = aDefaultRecurringJob().withCronExpression(Cron.daily()).build();
            storageProvider.saveRecurringJob(recurringJob);

            // WHEN
            runTask(task);

            // THEN recurringJob is scheduled ahead of time
            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasScheduledAt(zonedDateTime.plusDays(1).truncatedTo(DAYS).toInstant())
                    .hasRecurringJobId(recurringJob.getId());
        }

        // WHEN next run
        clearStorageProviderInvocationsAndCaptors();
        try (MockedStaticHolder ignored = mockTime(zonedDateTime.plusMinutes(1).withSecond(52))) {
            runTask(task);

            // THEN no recurring job is scheduled
            verify(storageProvider, never()).save(anyList());
        }

        // WHEN scheduled job instance is processed
        clearStorageProviderInvocationsAndCaptors();
        ZonedDateTime jobRunTime = zonedDateTime.plusDays(1).truncatedTo(DAYS);
        try (MockedStaticHolder ignored = mockTime(jobRunTime)) {
            runScheduledJobInstanceOfRecurringJob(recurringJob.getId());
        }

        // WHEN next run
        clearStorageProviderInvocationsAndCaptors();
        try (MockedStaticHolder ignored = mockTime(jobRunTime.withSecond(52))) {
            runTask(task);

            // THEN recurringJob is scheduled ahead of time again
            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasScheduledAt(jobRunTime.plusDays(1).toInstant())
                    .hasRecurringJobId(recurringJob.getId());
        }
    }

    @Test
    void testTaskSchedulesOneJobExtraAheadOfTimeForRecurringJobsUsingIntervalExpressionThatRunLessThanOncePerMinute() {
        ZonedDateTime zonedDateTime = FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR.atZone(ZoneId.systemDefault());
        RecurringJob recurringJob;
        try (MockedStaticHolder ignored = mockTime(zonedDateTime)) {
            // GIVEN
            recurringJob = aDefaultRecurringJob().withIntervalExpression("P1D").build();
            storageProvider.saveRecurringJob(recurringJob);

            // WHEN
            runTask(task);

            // THEN recurringJob is scheduled instantly
            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasScheduledAt(FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR.plus(1, DAYS))
                    .hasRecurringJobId(recurringJob.getId());
        }

        // WHEN scheduled job instance is processed
        clearStorageProviderInvocationsAndCaptors();
        ZonedDateTime jobRunTime = zonedDateTime.plusDays(1);
        try (MockedStaticHolder ignored = mockTime(jobRunTime)) {
            runScheduledJobInstanceOfRecurringJob(recurringJob.getId());
        }

        // WHEN next run
        clearStorageProviderInvocationsAndCaptors();
        try (MockedStaticHolder ignored = mockTime(jobRunTime.plusMinutes(1).withSecond(52))) {
            runTask(task);

            // THEN recurringJob is scheduled ahead of time
            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasScheduledAt(FIXED_INSTANT_ONE_MINUTE_AFTER_THE_HOUR.plus(2, DAYS))
                    .hasRecurringJobId(recurringJob.getId());
        }
    }

    @Test
    void taskSkipsAlreadyScheduledRecurringJobsOnStartup() {
        Instant now = now();
        RecurringJob recurringJob = aDefaultRecurringJob().withIntervalExpression("PT1H").build();

        storageProvider.saveRecurringJob(recurringJob);

        Instant lastScheduledAt = now().plusSeconds(6);

        // FIRST RUN - 1 job is already scheduled
        try (MockedStatic<Instant> ignored = mockTime(now)) {
            when(storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(lastScheduledAt);

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
        }

        // SECOND RUN - the job is still scheduled but will be moved to enqueued.
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval()))) {
            clearStorageProviderInvocationsAndCaptors();
            when(storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(lastScheduledAt);

            runTask(task);

            verify(storageProvider, times(0)).save(jobsToSaveArgumentCaptor.capture());
            verify(storageProvider, never()).getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING);
            assertThat(logger).hasNoInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range");
        }

        // THIRD RUN - the 1 scheduled job is no longer active
        try (MockedStatic<Instant> ignored = mockTime(now.plus(pollInterval().multipliedBy(2)))) {
            clearStorageProviderInvocationsAndCaptors();
            when(storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(null);

            runTask(task);

            assertThatSavedScheduledJobs()
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasRecurringJobId(recurringJob.getId());
        }
    }

    @Test
    void taskKeepsTrackOfRecurringJobRuns() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/15 * * * * *").build();

        when(storageProvider.recurringJobsUpdated(anyLong())).thenReturn(true);
        when(storageProvider.getRecurringJobs()).thenReturn(new RecurringJobsResult(List.of(recurringJob)));
        when(storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(null);

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            assertThatJobs(jobsToSaveArgumentCaptor.getAllValues().get(0))
                    .hasSize(1)
                    .allMatch(j -> j.getRecurringJobId().orElse("").equals(recurringJob.getId()))
                    .allMatch(j -> j.getState() == SCHEDULED);
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR.plus(pollInterval()))) {
            clearStorageProviderInvocationsAndCaptors();

            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
        }
    }

    @Test
    void taskDoesNotLogInfoMessageIfJobIsScheduledButYetToPassTheNextScheduledInstant() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression(Cron.minutely()).build();

        storageProvider.saveRecurringJob(recurringJob);

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.minus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, times(1)).save(jobsToSaveArgumentCaptor.capture());
            clearStorageProviderInvocationsAndCaptors();
        }

        when(storageProvider.getRecurringJobLatestScheduledInstant(recurringJob.getId(), AWAITING, SCHEDULED, ENQUEUED, PROCESSING)).thenReturn(FIXED_INSTANT_RIGHT_ON_THE_MINUTE);
        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE)) {
            runTask(task);

            verify(storageProvider, never()).save(anyList());
            assertThat(logger).hasNoInfoMessageContaining("Run will be skipped as job is taking longer than given CronExpression or Interval");
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.plus(pollInterval()))) {
            runTask(task);

            verify(storageProvider, never()).save(anyList());
            assertThat(logger).hasNoInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range 2022-12-14T08:36:00.500Z - 2022-12-14T08:36:05.500Z (PT5S) but it is already either AWAITING/SCHEDULED/ENQUEUED or PROCESSING and taking longer than given CronExpression or Interval. Run will be skipped.");
        }

        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_MINUTE.plusSeconds(60))) { // time skip
            runTask(task);

            verify(storageProvider, never()).save(anyList());
            assertThat(logger).hasInfoMessageContaining("Recurring job 'a recurring job' resulted in 1 scheduled jobs in time range 2022-12-14T08:36:55.500Z - 2022-12-14T08:37:00.500Z (PT5S) but it is already AWAITING, SCHEDULED, ENQUEUED or PROCESSING.");
        }
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void taskSchedulesJobsThatWereMissedDuringStopTheWorldGC() {
        RecurringJob recurringJob = aDefaultRecurringJob().withCronExpression("*/5 * * * * *").build();

        mockStorageProvider(List.of(recurringJob), emptyList());

        runTask(task);
        sleep(10500);
        runTask(task);

        assertThatSavedJobs(1)
                .hasSizeBetween(2, 3)
                .extracting(Job::getState)
                .containsOnly(SCHEDULED);
    }

    @SuppressWarnings("unchecked")
    private void clearStorageProviderInvocationsAndCaptors() {
        clearInvocations(storageProvider);
        jobsToSaveArgumentCaptor = ArgumentCaptor.forClass(List.class);
    }

    private void runScheduledJobInstanceOfRecurringJob(String recurringJobId) {
        var job = storageProvider.getJobList(SCHEDULED, Paging.AmountBasedList.ascOnUpdatedAt(10))
                .stream()
                .filter(j -> j.getRecurringJobId().map(recurringJobId::equals).orElse(false))
                .findFirst()
                .orElseThrow();
        job.enqueue();
        job.startProcessingOn(backgroundJobServer);
        job.succeeded();
        storageProvider.save(job);
    }

    private void mockStorageProvider(List<RecurringJob> recurringJobs, List<String> runningRecurringJobs) {
        recurringJobs.forEach(storageProvider::saveRecurringJob);
        mockRunningRecurringJob(runningRecurringJobs);
    }

    private void mockRunningRecurringJob(List<String> runningRecurringJobs) {
        Instant scheduledAt = runningRecurringJobs.isEmpty() ? null : Instant.now();
        lenient().doReturn(scheduledAt)
                .when(storageProvider).getRecurringJobLatestScheduledInstant(anyString(), eq(AWAITING), eq(SCHEDULED), eq(ENQUEUED), eq(PROCESSING));
    }

    IdListAssert<Job, JobAssert> assertThatSavedScheduledJobs() {
        return assertThatSavedJobs(null);
    }

    IdListAssert<Job, JobAssert> assertThatSavedJobs(Integer invocation) {
        List<Job> scheduledJobs = savedJobs(invocation);
        return assertThatJobs(scheduledJobs);
    }

    List<Job> savedJobs(Integer invocation) {
        VerificationMode verificationMode = invocation == null ? times(1) : atLeastOnce();

        verify(storageProvider, verificationMode).save(jobsToSaveArgumentCaptor.capture());
        return jobsToSaveArgumentCaptor.getAllValues().get(invocation == null ? 0 : invocation);
    }
}
