package org.jobrunr.jobs;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.*;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.resilience.Lock;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class JobTestBuilder {

    private UUID id;
    private Integer version;
    private String name;
    private JobDetails jobDetails;
    private List<JobState> states = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private Lock locker;

    private JobTestBuilder() {
    }

    public static JobTestBuilder aJob() {
        return new JobTestBuilder()
                .withId()
                .withName("the job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"));
    }

    public static JobTestBuilder aCopyOf(Job job) {
        return new JobTestBuilder()
                .withId(job.getId())
                .withName(job.getJobName())
                .withVersion(job.getVersion())
                .withLock(getInternalState(job, "locker"))
                .withJobDetails(job.getJobDetails())
                .withStates(job.getJobStates())
                .withMetadata(job.getMetadata());
    }

    private JobTestBuilder withStates(List<JobState> jobStates) {
        jobStates.forEach(this::withState);
        return this;
    }

    public static JobTestBuilder anEnqueuedJob() {
        return aJob()
                .withName("an enqueued job")
                .withState(new EnqueuedState());
    }

    public static JobTestBuilder anEnqueuedJobThatTakesLong() {
        return aJob()
                .withName("an enqueued job that takes long")
                .withState(new EnqueuedState())
                .withJobDetails(jobDetails()
                        .withClassName(TestService.class)
                        .withMethodName("doWorkThatTakesLong")
                        .withJobParameter(JobContext.Null));
    }

    public static JobTestBuilder aJobInProgress() {
        return anEnqueuedJob().withState(new ProcessingState(UUID.randomUUID()));
    }

    public static JobTestBuilder aScheduledJob() {
        return aJob()
                .withName("a scheduled job")
                .withState(new ScheduledState(now().minusSeconds(1)));
    }

    public static JobTestBuilder aFailedJob() {
        return anEnqueuedJob()
                .withName("a failed job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ProcessingState(UUID.randomUUID()))
                .withState(new FailedState("a message", new IllegalStateException()));
    }

    public static JobTestBuilder aSucceededJob() {
        return anEnqueuedJob()
                .withName("a succeeded job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ProcessingState(UUID.randomUUID()))
                .withState(new SucceededState(Duration.of(230, ChronoUnit.SECONDS), Duration.ofSeconds(10L, 7345L)));
    }

    public static JobTestBuilder aDeletedJob() {
        return anEnqueuedJob()
                .withName("a deleted job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new DeletedState("no reason"));
    }

    public static JobTestBuilder aFailedJobThatEventuallySucceeded() {
        final JobTestBuilder jobTestBuilder = aJob()
                .withName("failed job")
                .withJobDetails(defaultJobDetails())
                .withState(new ScheduledState(now().minusSeconds(11 * 60 * 60)));

        UUID serverId = UUID.randomUUID();
        for (int i = 0; i < 4; i++) {
            jobTestBuilder.withState(new EnqueuedState());
            jobTestBuilder.withState(new ProcessingState(serverId));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if(i < 3) {
                jobTestBuilder.withState(new ScheduledState(now().minusSeconds((10 - i) * 60 * 60), "Retry attempt " + (i + 1) + " of " + 10));
            }
        }
        jobTestBuilder.withState(new SucceededState(Duration.of(230, ChronoUnit.SECONDS), Duration.of(10, ChronoUnit.SECONDS)));

        return jobTestBuilder;
    }

    public static JobTestBuilder aFailedJobWithRetries() {
        final JobTestBuilder jobTestBuilder = aJob()
                .withName("failed job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ScheduledState(now().minusSeconds(11 * 60 * 60)));

        UUID serverId = UUID.randomUUID();
        for (int i = 0; i < 11; i++) {
            jobTestBuilder.withState(new EnqueuedState());
            jobTestBuilder.withState(new ProcessingState(serverId));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if(i < 10) {
                jobTestBuilder.withState(new ScheduledState(now().minusSeconds((10 - i) * 60 * 60), "Retry attempt " + (i + 1) + " of " + 10));
            }
        }

        return jobTestBuilder;
    }

    public JobTestBuilder withoutId() {
        id = null;
        return this;
    }

    public JobTestBuilder withId() {
        return withId(UUID.randomUUID());
    }

    public JobTestBuilder withId(UUID uuid) {
        this.id = uuid;
        return this;
    }

    public JobTestBuilder withVersion(int version) {
        this.version = version;
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

    public JobTestBuilder withLock(Lock lock) {
        this.locker = lock;
        return this;
    }

    public JobTestBuilder withJobDetails(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
        return this;
    }

    public JobTestBuilder withJobDetails(JobLambda jobLambda) {
        this.jobDetails = new CachingJobDetailsGenerator(new JobDetailsAsmGenerator()).toJobDetails(jobLambda);
        return this;
    }

    public JobTestBuilder withJobDetails(IocJobLambda jobLambda) {
        this.jobDetails = new CachingJobDetailsGenerator(new JobDetailsAsmGenerator()).toJobDetails(jobLambda);
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

    public JobTestBuilder withState(JobState state, Instant createdAt) {
        setInternalState(state, "createdAt", createdAt);
        if (state instanceof ProcessingState) setInternalState(state, "updatedAt", createdAt);
        this.states.add(state);
        return this;
    }

    public JobTestBuilder withEnqueuedState(Instant createdAt) {
        withState(new EnqueuedState(), createdAt);
        return this;
    }

    public JobTestBuilder withScheduledState() {
        return withState(new ScheduledState(now().minusSeconds(10)));
    }

    public JobTestBuilder withSucceededState() {
        return withState(new SucceededState(ofMillis(10), ofMillis(3)));
    }

    public JobTestBuilder withSucceededState(Instant createdAt) {
        return withState(new SucceededState(ofMillis(10), ofMillis(3)), createdAt);
    }

    public JobTestBuilder withFailedState() {
        return withState(new FailedState("Exception", new Exception()));
    }

    public JobTestBuilder withDeletedState() {
        return withState(new DeletedState("no reason"));
    }

    public JobTestBuilder withMetadata(String key, Object metadata) {
        this.metadata.put(key, metadata);
        return this;
    }

    public JobTestBuilder withMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata);
        return this;
    }

    public Job build() {
        Job job = new Job(jobDetails, states.remove(0));
        if (version != null) {
            Whitebox.setInternalState(job, "version", version);
        }
        if (locker != null) {
            Whitebox.setInternalState(job, "locker", locker);
        }
        job.setId(id);
        job.setJobName(name);
        job.getMetadata().putAll(metadata);

        ArrayList<JobState> jobHistory = getInternalState(job, "jobHistory");
        jobHistory.addAll(states);
        return job;
    }


}
