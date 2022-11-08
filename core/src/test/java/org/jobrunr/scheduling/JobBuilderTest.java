package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.details.JobDetailsGenerator;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.scheduling.JobBuilder.aJob;

class JobBuilderTest {

    private TestService testService;

    private JobDetailsGenerator jobDetailsGenerator = new JobDetailsAsmGenerator();

    @Test
    void testDefaultJob() {
        Job job = aJob()
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasId()
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithId() {
        UUID id = UUID.randomUUID();
        Job job = aJob()
                .withId(id)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasId(id)
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithJobName() {
        Job job = aJob()
                .withName("My job name")
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasJobName("My job name")
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithScheduleIn() {
        Job job = aJob()
                .scheduleIn(Duration.ofMinutes(1))
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(500, MILLIS));
    }

    @Test
    void testWithScheduleAt() {
        Job job = aJob()
                .scheduleAt(Instant.now().plusSeconds(60))
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(500, MILLIS));
    }

    @Test
    void testWithAmountOfRetries() {
        int amountOfRetries = 5;

        Job job = aJob()
                .withAmountOfRetries(amountOfRetries)
                .withDetails(() -> testService.doWork())
                .build(jobDetailsGenerator);

        assertThat(job)
                .hasAmountOfRetries(amountOfRetries)
                .hasState(StateName.ENQUEUED);
    }

}