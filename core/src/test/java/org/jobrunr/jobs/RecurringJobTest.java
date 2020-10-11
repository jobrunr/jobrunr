package org.jobrunr.jobs;

import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

class RecurringJobTest {

    @Test
    void onlyValidIdsAreAllowed() {
        assertThatCode(() -> aDefaultRecurringJob().withoutId().build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this-is-allowed-with-a-1").build()).doesNotThrowAnyException();
        assertThatCode(() -> aDefaultRecurringJob().withId("this_is_ALSO_allowed_with_a_2").build()).doesNotThrowAnyException();
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("this is not allowed").build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> aDefaultRecurringJob().withId("this-is-also-not-allowed-because-of-$").build()).isInstanceOf(IllegalArgumentException.class);
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
    void testToScheduledJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob().withName("the recurring job").build();

        final Job job = recurringJob.toScheduledJob();

        assertThat(job).hasJobName("the recurring job");
    }

    @Test
    void testToEnqueuedJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob().withName("the recurring job").build();

        final Job job = recurringJob.toEnqueuedJob();

        assertThat(job).hasJobName("the recurring job");
    }

    @Test
    void nextInstantIsCorrect() {
        LocalDateTime localDateTime = LocalDateTime.now();
        int hour = localDateTime.getHour();
        int minute = localDateTime.getMinute();
        if (localDateTime.getMinute() < 1) {
            hour--;
        } else {
            minute--;
        }
        final RecurringJob recurringJob = aDefaultRecurringJob()
                .withName("the recurring job")
                .withCronExpression(Cron.daily(hour, (minute)))
                .withZoneId(ZoneOffset.of("+02:00"))
                .build();
        Instant nextRun = recurringJob.getNextRun();
        assertThat(nextRun).isAfter(Instant.now());
    }
}