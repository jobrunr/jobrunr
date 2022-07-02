package org.jobrunr.scheduling;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.stubs.TestJobRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.scheduling.JobRequestBuilder.aJob;

class JobRequestBuilderTest {

    private final JobRequest jobRequest = new TestJobRequest("Not important");

    @Test
    void testDefaultJob() {
        Job job = aJob()
                .withDetails(jobRequest)
                .build();

        assertThat(job)
                .hasId()
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithJobName() {
        Job job = aJob()
                .withName("My job name")
                .withDetails(jobRequest)
                .build();

        assertThat(job)
                .hasJobName("My job name")
                .hasState(StateName.ENQUEUED);
    }

    @Test
    void testWithScheduleIn() {
        Job job = aJob()
                .scheduleIn(Duration.ofMinutes(1))
                .withDetails(jobRequest)
                .build();

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(200, MILLIS));
    }

    @Test
    void testWithScheduleAt() {
        Job job = aJob()
                .scheduleAt(Instant.now().plusSeconds(60))
                .withDetails(jobRequest)
                .build();

        assertThat(job).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = job.getJobState();
        assertThat(scheduledState.getScheduledAt()).isCloseTo(now().plusSeconds(60), within(200, MILLIS));
    }

    @Test
    void testWithAmountOfRetries() {
        int amountOfRetries = 5;

        Job job = aJob()
                .withAmountOfRetries(amountOfRetries)
                .withDetails(jobRequest)
                .build();

        assertThat(job)
                .hasAmountOfRetries(amountOfRetries)
                .hasState(StateName.ENQUEUED);
    }

}