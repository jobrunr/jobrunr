package org.jobrunr.jobs;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.details.CachingJobDetailsGenerator;
import org.jobrunr.jobs.details.JobDetailsAsmGenerator;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.DeletedState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.resilience.Lock;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.Optional.ofNullable;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.DEFAULT_SERVER_NAME;
import static org.jobrunr.utils.CollectionUtils.asSet;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

public class JobTestBuilder {

    public static List<Job>[] emptyJobList() {
        List<Job>[] result = cast(new ArrayList[1]);
        result[0] = new ArrayList<>();
        return result;
    }


    private UUID id;
    private Integer version;
    private String name;
    private Integer amountOfRetries;
    private String recurringJobId;
    private Set<String> labels;
    private JobDetails jobDetails;
    private List<JobState> states = new ArrayList<>();
    private Map<String, Object> metadata = new HashMap<>();
    private Lock locker;
    private boolean withoutStateChanges = true;

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
        return anEnqueuedJob().withState(new ProcessingState(UUID.randomUUID(), DEFAULT_SERVER_NAME));
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
                .withState(new ProcessingState(UUID.randomUUID(), DEFAULT_SERVER_NAME))
                .withState(new FailedState("a message", new IllegalStateException()));
    }

    public static JobTestBuilder aSucceededJob() {
        return anEnqueuedJob()
                .withName("a succeeded job")
                .withJobDetails(systemOutPrintLnJobDetails("a test"))
                .withState(new ProcessingState(UUID.randomUUID(), DEFAULT_SERVER_NAME))
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
            jobTestBuilder.withState(new ProcessingState(serverId, DEFAULT_SERVER_NAME));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if (i < 3) {
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
            jobTestBuilder.withState(new ProcessingState(serverId, DEFAULT_SERVER_NAME));
            jobTestBuilder.withState(new FailedState("An exception occurred", new IllegalStateException()));
            if (i < 10) {
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
        return withId(Job.newUUID());
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

    public JobTestBuilder withAmountOfRetries(int amountOfRetries) {
        this.amountOfRetries = amountOfRetries;
        return this;
    }

    public JobTestBuilder withRecurringJobId(String recurringJobId) {
        this.recurringJobId = recurringJobId;
        return this;
    }

    public JobTestBuilder withLabels(String... labels) {
        return withLabels(asSet(labels));
    }

    public JobTestBuilder withLabels(Set<String> labels) {
        this.labels = labels;
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
        this.jobDetails = new CachingJobDetailsGenerator(new JobDetailsAsmGenerator()).toJobDetails(jobLambda);
        return this;
    }

    public <S> JobTestBuilder withJobDetails(IocJobLambda<S> jobLambda) {
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

    public JobTestBuilder withProcessingState() {
        return withState(new ProcessingState(UUID.randomUUID(), DEFAULT_SERVER_NAME));
    }

    public JobTestBuilder withProcessingState(UUID backgroundJobServerId) {
        return withState(new ProcessingState(backgroundJobServerId, DEFAULT_SERVER_NAME));
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

    public JobTestBuilder withLock(Lock lock) {
        this.locker = lock;
        return this;
    }

    public JobTestBuilder withInitialStateChanges() {
        this.withoutStateChanges = false;
        return this;
    }

    public Job build() {
        Job job = new Job(id, ofNullable(this.version).orElse(0), jobDetails, states, new ConcurrentHashMap<>(metadata));
        if (locker != null) {
            Whitebox.setInternalState(job, "locker", locker);
        }
        if (amountOfRetries != null) {
            job.setAmountOfRetries(amountOfRetries);
        }
        if (labels != null) {
            job.setLabels(labels);
        }
        job.setJobName(name);
        job.setRecurringJobId(recurringJobId);

        if(withoutStateChanges) {
            job.getStateChangesForJobFilters(); // reset state changes
        }
        return job;
    }
}
