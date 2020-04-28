package org.jobrunr.jobs;

import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.lambdas.JobWithoutIoc;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.stubs.TestService;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class JobTestBuilder {

    private UUID id;
    private String name;
    private JobDetails jobDetails;
    private List<JobState> states = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();

    private JobTestBuilder() {
    }

    public static JobTestBuilder aJob() {
        return new JobTestBuilder()
                .withName("the job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"));
    }

    public static JobTestBuilder anEnqueuedJob() {
        return aJob()
                .withoutId()
                .withName("an enqueued job")
                .withState(new EnqueuedState());
    }

    public static JobTestBuilder anEnqueuedJobThatTakesLong() {
        return aJob()
                .withoutId()
                .withName("an enqueued job that takes long")
                .withState(new EnqueuedState())
                .withJobDetails(jobDetails()
                        .withLambdaType(JobWithoutIoc.class)
                        .withClassName(TestService.class)
                        .withMethodName("doWorkThatTakesLong"));
    }

    public static JobTestBuilder aScheduledJob() {
        return aJob()
                .withId()
                .withName("a scheduled job")
                .withState(new ScheduledState(Instant.now().minusSeconds(1)));
    }

    public static JobTestBuilder aFailedJob() {
        return anEnqueuedJob()
                .withId()
                .withName("a failed job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ProcessingState(UUID.randomUUID()))
                .withState(new FailedState("a message", new IllegalStateException()));
    }

    public static JobTestBuilder aSucceededJob() {
        return anEnqueuedJob()
                .withId()
                .withName("a succeeded job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ProcessingState(UUID.randomUUID()))
                .withState(new SucceededState(Duration.of(230, ChronoUnit.SECONDS), Duration.ofSeconds(10L, 7345L)));
    }

    public static JobTestBuilder aFailedJobThatEventuallySucceeded() {
        final JobTestBuilder jobTestBuilder = aJob()
                .withId()
                .withName("failed job")
                .withJobDetails(defaultJobDetails())
                .withState(new ScheduledState(Instant.now().minusSeconds(11 * 60 * 60)));

        UUID serverId = UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            jobTestBuilder.withState(new EnqueuedState());
            jobTestBuilder.withState(new ProcessingState(serverId));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if(i < 3) {
                jobTestBuilder.withState(new ScheduledState(Instant.now().minusSeconds((10 - i) * 60 * 60), "Retry attempt " + (i + 1) + " of " + 10));
            }
        }
        jobTestBuilder.withState(new SucceededState(Duration.of(230, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS)));

        return jobTestBuilder;
    }

    public static JobTestBuilder aFailedJobWithRetries() {
        final JobTestBuilder jobTestBuilder = aJob()
                .withId()
                .withName("failed job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ScheduledState(Instant.now().minusSeconds(11 * 60 * 60)));

        UUID serverId = UUID.randomUUID();
        for (int i = 0; i < 11; i++) {
            jobTestBuilder.withState(new EnqueuedState());
            jobTestBuilder.withState(new ProcessingState(serverId));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if(i < 10) {
                jobTestBuilder.withState(new ScheduledState(Instant.now().minusSeconds((10 - i) * 60 * 60), "Retry attempt " + (i + 1) + " of " + 10));
            }
        }

        return jobTestBuilder;
    }

    public JobTestBuilder withoutId() {
        id = null;
        return this;
    }

    public JobTestBuilder withId() {
        this.id = UUID.randomUUID();
        return this;
    }

    public JobTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public JobTestBuilder withoutName() {
        this.name = null;
        return this;
    }

    public JobTestBuilder withJobDetails(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
        return this;
    }

    public JobTestBuilder withJobDetails(JobLambda jobLambda) {
        this.jobDetails = new JobDetailsAsmGenerator().toJobDetails(jobLambda);
        return this;
    }

    public JobTestBuilder withJobDetails(JobDetailsTestBuilder jobDetailsTestBuilder) {
        this.jobDetails = jobDetailsTestBuilder.build();
        return this;
    }

    public JobTestBuilder withState(JobState state) {
        this.states.add(state);
        return this;
    }

    public JobTestBuilder withEnqueuedState(Instant instant) {
        EnqueuedState enqueuedState = new EnqueuedState();
        setInternalState(enqueuedState, "createdAt", instant);
        this.states.add(enqueuedState);
        return this;
    }

    public JobTestBuilder withMetadata(String key, Object metadata) {
        this.metadata.put(key, metadata);
        return this;
    }

    public Job build() {
        Job job = new Job(jobDetails, states.remove(0));
        job.setId(id);
        job.setJobName(name);
        job.getMetadata().putAll(metadata);

        ArrayList<JobState> jobHistory = Whitebox.getInternalState(job, "jobHistory");
        for (JobState jobstate : states) {
            jobHistory.add(jobstate);
        }
        return job;
    }
}
