package org.jobrunr.scheduling;

import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.carbonaware.CarbonAware;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.stubs.TestInvalidJobRequest;
import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceInterface;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.RecurringJob.CreatedBy.API;
import static org.jobrunr.scheduling.RecurringJobBuilder.aRecurringJob;

class RecurringJobBuilderTest {

    private static final String every5Seconds = "*/5 * * * * *";
    private static final String duration1Minute = "PT1M";

    private final JobDetailsGenerator jobDetailsGenerator = new JobDetailsAsmGenerator();
    private final JobRequest jobRequest = new TestJobRequest("Not important");

    private TestService testService;

    @Test
    void testDefaultJobWithJobLambda() {
        RecurringJob recurringJob = aRecurringJob()
                .withDetails(() -> testService.doWork())
                .withCron(every5Seconds)
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression(every5Seconds)
                .hasJobDetails(TestService.class, "doWork")
                .hasCreatedBy(API);
    }

    @Test
    void testDefaultJobWithJobLambdaUsingInterface() {
        RecurringJob recurringJob = aRecurringJob()
                .withDetails(TestServiceInterface::doWork)
                .withCron(every5Seconds)
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression(every5Seconds)
                .hasJobDetails(TestServiceInterface.class, "doWork")
                .hasCreatedBy(API);
    }

    @Test
    void testDefaultJobWithIoCJobLambda() {
        RecurringJob recurringJob = aRecurringJob()
                .<TestService>withDetails(TestService::doWork)
                .withCron(every5Seconds)
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression(every5Seconds)
                .hasJobDetails(TestService.class, "doWork");
    }

    @Test
    void testDefaultJobWithJobRequest() {
        RecurringJob recurringJob = aRecurringJob()
                .withJobRequest(jobRequest)
                .withCron(every5Seconds)
                .build();

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression(every5Seconds)
                .hasJobDetails(TestJobRequest.TestJobRequestHandler.class, "run", jobRequest)
                .hasCreatedBy(API);
    }

    @Test
    void testJobRequestWithInvalidJobRequest() {
        assertThatCode(() -> aRecurringJob()
                .withJobRequest(new TestInvalidJobRequest())
                .withCron(every5Seconds)
                .build())
                .isInstanceOf(JobMethodNotFoundException.class);
    }

    @Test
    void testWithId() {
        String id = UUID.randomUUID().toString();
        RecurringJob recurringJob = aRecurringJob()
                .withId(id)
                .withDetails(() -> testService.doWork())
                .withCron(every5Seconds)
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId(id)
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testWithJobName() {
        RecurringJob recurringJob = aRecurringJob()
                .withDetails(() -> testService.doWork())
                .withCron(every5Seconds)
                .withName("My job name")
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasJobName("My job name")
                .hasId()
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testWithAmountOfRetries() {
        RecurringJob recurringJob = aRecurringJob()
                .withAmountOfRetries(10)
                .withCron(every5Seconds)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasRetries(10)
                .hasId()
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testWithLabels() {
        RecurringJob recurringJob = aRecurringJob()
                .withLabels(List.of("TestLabel", "Email"))
                .withCron(every5Seconds)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasLabels(List.of("TestLabel", "Email"))
                .hasId()
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testMaxAmountOfLabels() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withLabels("TestLabel", "Email", "Automated", "Too many")
                        .withCron(every5Seconds)
                        .withDetails(() -> testService.doWork())
                        .build(jobDetailsGenerator));
    }

    @Test
    void testMaxLengthOfLabel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withLabels("Label longer than 45 characters should throw an exception")
                        .withCron(every5Seconds)
                        .withDetails(() -> testService.doWork())
                        .build(jobDetailsGenerator));
    }

    @Test
    void testWithInterval() {
        RecurringJob recurringJob = aRecurringJob()
                .withInterval(Duration.ofMinutes(1))
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression(duration1Minute);
    }

    @Test
    void testWithScheduleExpression() {
        RecurringJob recurringJob = aRecurringJob()
                .withScheduleExpression(CarbonAware.dailyBefore(7))
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasId()
                .hasScheduleExpression("0 7 * * * [PT7H/PT0S]");
    }

    @Test
    void testWithZoneId() {
        RecurringJob recurringJob = aRecurringJob()
                .withZoneId(ZoneId.of("Europe/Brussels"))
                .withCron(every5Seconds)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasZoneId("Europe/Brussels")
                .hasId()
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testWithDefaultZoneId() {
        RecurringJob recurringJob = aRecurringJob()
                .withCron(every5Seconds)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(recurringJob)
                .hasZoneId(systemDefault().toString())
                .hasId()
                .hasScheduleExpression(every5Seconds);
    }

    @Test
    void testJobDetailsCanOnlyBeSet1Way() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withDetails(() -> testService.doWork())
                        .withJobRequest(jobRequest)
                        .withCron(every5Seconds)
                        .build(jobDetailsGenerator));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withJobRequest(jobRequest)
                        .withDetails(() -> testService.doWork())
                        .withCron(every5Seconds)
                        .build(jobDetailsGenerator));
    }

    @Test
    void testBuildWithIncorrectJobDetails() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withJobRequest(jobRequest)
                        .withCron(every5Seconds)
                        .build(jobDetailsGenerator));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withDetails(() -> testService.doWork())
                        .withCron(every5Seconds)
                        .build());
    }

    @Test
    void testBuildWithoutSchedule() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withJobRequest(jobRequest)
                        .build());
    }

    @Test
    void scheduleCanOnlyBeSet1Way() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withCron(every5Seconds)
                        .withInterval(Duration.ofMinutes(1))
                        .withJobRequest(jobRequest)
                        .build());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withInterval(Duration.ofMinutes(1))
                        .withCron(every5Seconds)
                        .withJobRequest(jobRequest)
                        .build());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> aRecurringJob()
                        .withScheduleExpression(CarbonAware.dailyBefore(10))
                        .withInterval(Duration.ofMinutes(1))
                        .withCron(every5Seconds)
                        .withJobRequest(jobRequest)
                        .build());
    }
}
