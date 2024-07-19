package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.carbonaware.CarbonAware;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.recurringjobs.insomeverylongpackagename.with.nestedjobrequests.SimpleJobRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThatJobs;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.InstantMocker.FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR;
import static org.mockito.InstantMocker.mockTime;

class RecurringJobTest {

    @Test
    void onlyValidIdsAreAllowed() {
        assertThatCode(() -> aDefaultRecurringJob().withoutId().build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this-is-allowed-with-a-1").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this_is_ALSO_allowed_with_a_2").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this_is_ALSO_allowed_with_a_2").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("some-id".repeat(20).substring(0, 127)).build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withoutId().withJobDetails(new JobDetails(new SimpleJobRequest())).build()).doesNotThrowAnyException();
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("some-id".repeat(20)).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("this is not allowed").build()).isInstanceOf(IllegalArgumentException.class);
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
    void testToScheduledJobWith1FutureRun() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("*/5 * * * * *")
                .withId("the-recurring-job")
                .withName("the recurring job")
                .build();

        final List<Job> jobs = recurringJob.toJobsWith1FutureRun(now(), now().plusSeconds(5));

        assertThatJobs(jobs)
                .hasSize(2)
                .allMatch(job -> job.getRecurringJobId().orElse("other-id").equals("the-recurring-job"))
                .allMatch(job -> job.getState().equals(SCHEDULED));
    }

    @Test
    void testToJobsWith1FutureRunWithMissedRunsShouldCreateMultipleJobs() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("*/5 * * * * *")
                .build();

        Instant from = now().minusSeconds(15);
        final List<Job> jobs = recurringJob.toJobsWith1FutureRun(from, now());

        assertThatJobs(jobs).hasSize(4)
                .allMatch(job -> job.getRecurringJobId().orElse("other-id").equals(recurringJob.getId()))
                .allMatch(job -> job.getState().equals(SCHEDULED))
                .allMatch(job -> ((ScheduledState) job.getJobState()).getScheduledAt().isAfter(from));
    }

    @Test
    void testToJobsWith1FutureRunShouldReturnOnly1FutureJobWhenJobScheduleDoesNotFitInTimeWindow() {
        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            final RecurringJob recurringJob = aDefaultRecurringJob()
                    .withCronExpression(Cron.weekly())
                    .build();

            final List<Job> jobs = recurringJob.toJobsWith1FutureRun(now(), now());

            assertThat(jobs).hasSize(1);
        }
    }

    @Test
    void testToJobsWith1FutureRunGetsAllJobsBetweenStartAndNowMultipleResults() {
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withCronExpression("*/5 * * * * *")
                .build();

        final List<Job> jobs = recurringJob.toJobsWith1FutureRun(now().minusSeconds(15), now().plusSeconds(5));

        assertThat(jobs).hasSize(5);
    }

    @Test
    void testToJobsWith1FutureRunCorrectlySetsTheInitialState() {
        try (MockedStatic<Instant> ignored = mockTime(FIXED_INSTANT_RIGHT_BEFORE_THE_HOUR)) {
            final RecurringJob classicRecurringJob = aDefaultRecurringJob()
                    .withCronExpression("*/5 * * * * *")
                    .build();

            final List<Job> classicJobs = classicRecurringJob.toJobsWith1FutureRun(now(), now().plusSeconds(5));

            assertThatJobs(classicJobs)
                    .hasSize(2)
                    .allMatch(job -> job.getJobState() instanceof ScheduledState);

            final RecurringJob carbonAwareRecurringJob = aDefaultRecurringJob()
                    .withCronExpression(CarbonAware.using(Cron.daily(7), Duration.ofHours(4), Duration.ZERO))
                    .build();

            final List<Job> carbonAwareRecurringJobs = carbonAwareRecurringJob.toJobsWith1FutureRun(now(), now().plusSeconds(5));

            assertThatJobs(carbonAwareRecurringJobs)
                    .hasSize(1)
                    .allMatch(job -> job.getJobState() instanceof CarbonAwareAwaitingState);

        }
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
                .hasLabels(Set.of("some label"));
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