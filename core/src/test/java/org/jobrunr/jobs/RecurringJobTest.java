package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.recurringjobs.insomeverylongpackagename.with.nestedjobrequests.SimpleJobRequest;
import org.junit.jupiter.api.Test;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static java.time.Instant.now;
import static java.time.Instant.parse;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Index.atIndex;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_AT_THE_HOUR;
import static org.mockito.InstantMocker.mockTime;

class RecurringJobTest {

    @Test
    void onlyValidIdsAreAllowed() {
        assertThatCode(() -> aDefaultRecurringJob().withoutId().build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this-is-allowed-with-a-1").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this_is_ALSO_allowed_with_a_2").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("some-id" .repeat(20).substring(0, 127)).build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withoutId().withJobDetails(new JobDetails(new SimpleJobRequest())).build()).doesNotThrowAnyException();
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("some-id" .repeat(20)).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("this is not allowed").build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("this-is-also-not-allowed-because-of-$").build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recurringJobWithAVeryLongIdUsesMD5HashingForId() {
        RecurringJob recurringJob = aDefaultRecurringJob().withoutId().withJobDetails(new JobDetails(new SimpleJobRequest())).build();
        assertThat(recurringJob.getId()).isEqualTo("045101544c9006c596e6bc7c59506913");
    }

    @Test
    void ifNoIdGivenItUsesJobSignature() {
        TestService testService = new TestService();
        final RecurringJob recurringJob1 = aDefaultRecurringJob().withoutId().withJobDetails(() -> System.out.println("This is a test")).build();
        assertThat(recurringJob1.getId()).isEqualTo("java.lang.System.out.println(java.lang.String)");

        IocJobLambda<TestService> iocJobLambda = (x) -> x.doWork(3, 97693);
        final RecurringJob recurringJob2 = aDefaultRecurringJob().withoutId().withJobDetails(iocJobLambda).build();
        assertThat(recurringJob2.getId()).isEqualTo("org.jobrunr.stubs.TestService.doWork(java.lang.Integer,java.lang.Integer)");

        final RecurringJob recurringJob3 = aDefaultRecurringJob().withoutId().withJobDetails((JobLambda) testService::doWork).build();
        assertThat(recurringJob3.getId()).isEqualTo("org.jobrunr.stubs.TestService.doWork()");
    }

    @Test
    void testToScheduledJobsGetsOneJobBetweenFromAndUpFromIsInclusiveAndEndIsExclusive() {
        try (MockedStatic<Instant> ignored = mockTime(InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            final RecurringJob recurringJob = aDefaultRecurringJob()
                    .withCronExpression("* * * * *")
                    .build();

            final List<Job> jobs = recurringJob.toScheduledJobs(FIXED_INSTANT_RIGHT_AT_THE_HOUR, FIXED_INSTANT_RIGHT_AT_THE_HOUR.plusSeconds(60));

            assertThatJobs(jobs)
                    .singleElement()
                    .hasState(SCHEDULED)
                    .hasScheduledAt(FIXED_INSTANT_RIGHT_AT_THE_HOUR);
        }
    }

    @Test
    void testToScheduledJobsGetsOneJobBetweenFromAndUpFromIsInclusiveAndEndIsExclusiveThreeIntervals() {
        try (MockedStatic<Instant> ignored = mockTime(InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            final RecurringJob recurringJob = aDefaultRecurringJob()
                    .withCronExpression("* * * * *")
                    .build();

            final List<Job> jobs = recurringJob.toScheduledJobs(FIXED_INSTANT_RIGHT_AT_THE_HOUR, FIXED_INSTANT_RIGHT_AT_THE_HOUR.plusSeconds(130));

            assertThatJobs(jobs)
                    .hasSize(3)
                    .satisfies(job -> assertThat(job).hasState(SCHEDULED).hasScheduledAt(FIXED_INSTANT_RIGHT_AT_THE_HOUR), atIndex(0))
                    .satisfies(job -> assertThat(job).hasState(SCHEDULED).hasScheduledAt(FIXED_INSTANT_RIGHT_AT_THE_HOUR.plusSeconds(60)), atIndex(1))
                    .satisfies(job -> assertThat(job).hasState(SCHEDULED).hasScheduledAt(FIXED_INSTANT_RIGHT_AT_THE_HOUR.plusSeconds(120)), atIndex(2));
        }
    }


    @Test
    void testCreateARecurringCronJobWithCarbonAwareMarginSetsScheduleCorrectly() {
        var cron = aDefaultRecurringJob()
                .withId("cron")
                .withCronExpression("0 0 1 * * [PT1H/PT10H]")
                .build();

        assertThat(cron).hasScheduleExpression("0 0 1 * * [PT1H/PT10H]");
    }

    @Test
    void testCreateARecurringIntervalJobWithCarbonAwareMarginSetsScheduleCorrectly() {
        var interval = aDefaultRecurringJob()
                .withId("interval")
                .withIntervalExpression("PT1H [PT4H/PT0S]")
                .build();

        assertThat(interval).hasScheduleExpression("PT1H [PT4H/PT0S]");
    }

    @Test
    void testToScheduledJobsGetsAllJobsBetweenStartAndEnd() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("*/5 * * * * *")
                .build();

        final List<Job> jobs = recurringJob.toScheduledJobs(now(), now().plusSeconds(5));

        assertThat(jobs).hasSize(1);
        ScheduledState scheduledState = jobs.get(0).getJobState();
        assertThat(scheduledState.getScheduledAt()).isAfter(now());
    }

    @Test
    void testToScheduledJobsGetsAllJobsBetweenStartAndEndNoResultsThenReturnsJobScheduledAheadOfTime() {
        try (MockedStatic<Instant> ignored = mockTime(parse("2025-04-02T00:00:00Z"))) {
            final RecurringJob recurringJob = aDefaultRecurringJob()
                    .withCronExpression(Cron.weekly())
                    .withZoneId(ZoneOffset.UTC)
                    .build();

            final List<Job> jobs = recurringJob.toScheduledJobs(now(), now().plusSeconds(5));

            assertThat(jobs).hasSize(1);
            assertThat(jobs.get(0))
                    .hasRecurringJobId(recurringJob.getId())
                    .hasState(SCHEDULED)
                    .hasScheduledAt(Instant.parse("2025-04-07T00:00:00Z"));
        }
    }

    @Test
    void testToScheduledJobsGetsAllJobsBetweenStartAndEndMultipleResults() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("*/5 * * * * *")
                .build();

        final List<Job> jobs = recurringJob.toScheduledJobs(now().minusSeconds(15), now().plusSeconds(5));

        assertThat(jobs).hasSize(4);
    }

    @Test
    void testToScheduledJobsForCarbonAwareAheadOfTimeCreatesAJobInAwaitingState() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("0 0 1 * * [PT1H/PT10H]")
                .build();

        final List<Job> jobs = recurringJob.toScheduledJobs(now(), now().plusSeconds(5));

        assertThat(jobs).hasSize(1);
        CarbonAwareAwaitingState awaitingState = jobs.get(0).getJobState();
        assertThat(awaitingState.getReason()).isEqualTo("Ahead of time by recurring job 'a recurring job'");
    }

    @Test
    void testToScheduledJobsForCarbonAwareCreatesAJobInAwaitingState() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("0 13 * * * [PT1H/PT10H]")
                .build();

        final List<Job> jobs = recurringJob.toScheduledJobs(Instant.parse("2024-11-20T09:00:00.000Z"), Instant.parse("2024-11-21T09:00:00.000Z"));

        assertThat(jobs).hasSize(1);
        CarbonAwareAwaitingState awaitingState = jobs.get(0).getJobState();
        assertThat(awaitingState.getReason()).isEqualTo("By recurring job 'a recurring job'");
    }

    @Test
    void testToScheduledJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withId("the-recurring-job")
                .withName("the recurring job")
                .build();

        final Job job = recurringJob.toScheduledJob();

        assertThat(job)
                .hasRecurringJobId("the-recurring-job")
                .hasJobName("the recurring job")
                .hasState(SCHEDULED);
    }

    @Test
    void testToEnqueuedJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withId("the-recurring-job")
                .withName("the recurring job")
                .withAmountOfRetries(3)
                .withLabels("some label")
                .build();

        final Job job = recurringJob.toEnqueuedJob();

        assertThat(job)
                .hasRecurringJobId("the-recurring-job")
                .hasJobName("the recurring job")
                .hasState(ENQUEUED)
                .hasAmountOfRetries(3)
                .hasLabels(List.of("some label"));
    }

    @Test
    void nextInstantWithCronExpressionIsCorrect() {
        LocalDateTime localDateTime = LocalDateTime.now();
        LocalDateTime timeForCron = localDateTime.plusMinutes(-1);

        int hour = timeForCron.getHour();
        int minute = timeForCron.getMinute();

        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withName("the recurring job")
                .withCronExpression(Cron.daily(hour, (minute)))
                .withZoneId(ZoneOffset.of("+02:00"))
                .build();
        Instant nextRun = recurringJob.getNextRun();
        assertThat(nextRun).isAfter(now());
    }

    @Test
    void nextInstantWithIntervalIsCorrect() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withName("the recurring job")
                .withIntervalExpression(Duration.ofHours(1).toString())
                .withZoneId(ZoneOffset.of("+02:00"))
                .build();
        Instant nextRun = recurringJob.getNextRun();
        assertThat(nextRun).isAfter(now());
    }
}