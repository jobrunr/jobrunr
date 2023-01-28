package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.stubs.TestJobRequestWithoutJobAnnotation;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.scheduling.JobBuilder.aJob;

class JobBuilderTest {

    private final JobDetailsGenerator jobDetailsGenerator = new JobDetailsAsmGenerator();
    private final JobRequest jobRequest = new TestJobRequestWithoutJobAnnotation("Not important");

    private TestService testService;

    @Test
    void testJobBuilderCannotBeCombinedWithAnnotation() {
        assertThatThrownBy(() ->  aJob()
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are combining the JobBuilder with the Job annotation which is not allowed. You can only use one of them.");
    }

    @Test
    void testDefaultJobWithJobLambda() {
        UUID uuid = UUID.randomUUID();
        Job job = aJob()
                .withDetails(() -> testService.doWorkWithUUID(uuid))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasId()
                .hasJobDetails(TestService.class, "doWorkWithUUID", uuid)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testDefaultJobWithIoCJobLambda() {
        UUID uuid = UUID.randomUUID();
        Job job = aJob()
                .<TestService>withDetails(x -> x.doWorkWithUUID(uuid))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasId()
                .hasJobDetails(TestService.class, "doWorkWithUUID", uuid)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testDefaultJobWithJobRequest() {
        Job job = aJob()
                .withJobRequest(jobRequest)
                .build();

        assertThat(job)
                .hasId()
                .hasJobDetails(TestJobRequestWithoutJobAnnotation.TestWithoutJobAnnotationJobRequestHandler.class, "run", jobRequest)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithId() {
        UUID id = UUID.randomUUID();
        Job job = aJob()
                .withId(id)
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasId(id)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithJobName() {
        Job job = aJob()
                .withName("My job name")
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasJobName("My job name")
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithScheduleIn() {
        Job job = aJob()
                .scheduleIn(Duration.ofMinutes(1))
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(500, MILLIS));
    }

    @Test
    void testWithScheduleAt() {
        Job job = aJob()
                .scheduleAt(Instant.now().plusSeconds(60))
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(500, MILLIS));
    }

    @Test
    void testThatOnlyOneOfScheduleInScheduleIsAllowed() {
        assertThatThrownBy(() -> aJob().scheduleAt(Instant.now()).scheduleIn(Duration.ZERO).build(jobDetailsGenerator))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> aJob().scheduleIn(Duration.ZERO).scheduleAt(Instant.now()).build(jobDetailsGenerator))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testWithAmountOfRetries() {
        int amountOfRetries = 5;

        Job job = aJob()
                .withAmountOfRetries(amountOfRetries)
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasAmountOfRetries(amountOfRetries)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithLabels() {
        Job job = aJob()
                .withLabels(Set.of("TestLabel", "Email"))
                .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasLabels(Set.of("TestLabel", "Email"))
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testMaxAmountOfLabels() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withLabels("TestLabel", "Email", "Automated", "Too many")
                        .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                        .build(jobDetailsGenerator));
    }

    @Test
    void testMaxLengthOfLabel() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withLabels("Label longer than 45 characters should throw an exception")
                        .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                        .build(jobDetailsGenerator));
    }

    @Test
    void testJobDetailsCanOnlyBeSet1Way() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withJobRequest(jobRequest)
                        .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID())));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                        .withJobRequest(jobRequest));
    }

    @Test
    void testBuildWithIncorrectJobDetails() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withJobRequest(jobRequest)
                        .build(jobDetailsGenerator));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> aJob()
                        .withDetails(() -> testService.doWorkWithUUID(UUID.randomUUID()))
                        .build());
    }
}