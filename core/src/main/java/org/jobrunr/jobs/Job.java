package org.jobrunr.jobs;

import org.jobrunr.jobs.states.*;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.utils.streams.StreamUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.jobs.states.AllowedJobStateStateChanges.isIllegalStateChange;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

/**
 * Defines the job with its JobDetails, History and Job Metadata
 */
public class Job extends AbstractJob {

    private UUID id;
    private ArrayList<JobState> jobHistory;
    private final ConcurrentMap<String, Object> metadata;

    private Job() {
        // used for deserialization
        this.metadata = new ConcurrentHashMap<>();
    }

    public Job(JobDetails jobDetails) {
        this(jobDetails, new EnqueuedState());
    }

    public Job(UUID id, JobDetails jobDetails) {
        this(id, jobDetails, new EnqueuedState());
    }

    public Job(JobDetails jobDetails, JobState jobState) {
        this(jobDetails, singletonList(jobState));
    }

    public Job(UUID id, JobDetails jobDetails, JobState jobState) {
        this(id, jobDetails, singletonList(jobState));
    }

    public Job(JobDetails jobDetails, List<JobState> jobHistory) {
        this(null, 0, jobDetails, jobHistory, new ConcurrentHashMap<>());
    }

    public Job(UUID id, JobDetails jobDetails, List<JobState> jobHistory) {
        this(id, 0, jobDetails, jobHistory, new ConcurrentHashMap<>());
    }

    public Job(UUID id, int version, JobDetails jobDetails, List<JobState> jobHistory, ConcurrentMap<String, Object> metadata) {
        super(jobDetails, version);
        if (jobHistory.isEmpty()) throw new IllegalStateException("A job should have at least one initial state");
        this.id = id != null ? id : UUID.randomUUID();
        this.jobHistory = new ArrayList<>(jobHistory);
        this.metadata = metadata;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getId() {
        return id;
    }

    public List<JobState> getJobStates() {
        return unmodifiableList(jobHistory);
    }

    public <T extends JobState> Stream<T> getJobStatesOfType(Class<T> clazz) {
        return StreamUtils.ofType(getJobStates(), clazz);
    }

    public <T extends JobState> Optional<T> getLastJobStateOfType(Class<T> clazz) {
        return getJobStatesOfType(clazz).reduce((first, second) -> second);
    }

    public <T extends JobState> T getJobState() {
        return cast(getJobState(-1));
    }

    public JobState getJobState(int element) {
        if (element >= 0) {
            return jobHistory.get(element);
        } else {
            if (Math.abs(element) > jobHistory.size()) return null;
            return jobHistory.get(jobHistory.size() + element);
        }
    }

    public StateName getState() {
        return getJobState().getName();
    }

    public void addJobState(JobState jobState) {
        if (isIllegalStateChange(getState(), jobState.getName())) {
            throw new IllegalJobStateChangeException(getState(), jobState.getName());
        }
        this.jobHistory.add(jobState);
    }

    public boolean hasState(StateName state) {
        return getState().equals(state);
    }

    public void enqueue() {
        addJobState(new EnqueuedState());
    }

    public void scheduleAt(Instant instant, String reason) {
        addJobState(new ScheduledState(instant, reason));
    }

    public void startProcessingOn(BackgroundJobServer backgroundJobServer) {
        if (getState() == StateName.PROCESSING) throw new ConcurrentJobModificationException(this);
        addJobState(new ProcessingState(backgroundJobServer.getId()));
    }

    public void updateProcessing() {
        ProcessingState jobState = getJobState();
        jobState.setUpdatedAt(Instant.now());
    }

    public void succeeded() {
        Optional<EnqueuedState> lastEnqueuedState = getLastJobStateOfType(EnqueuedState.class);
        if (!lastEnqueuedState.isPresent()) {
            throw new IllegalStateException("Job cannot succeed if it was not enqueued before.");
        }

        Duration latencyDuration = Duration.between(lastEnqueuedState.get().getEnqueuedAt(), getJobState().getCreatedAt());
        Duration processDuration = Duration.between(getJobState().getCreatedAt(), Instant.now());
        addJobState(new SucceededState(latencyDuration, processDuration));
    }

    public void failed(String message, Exception exception) {
        addJobState(new FailedState(message, exception));
    }

    public void delete(String reason) {
        addJobState(new DeletedState(reason));
    }

    public Instant getCreatedAt() {
        return getJobState(0).getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return getJobState().getUpdatedAt();
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "Job{" +
                "id=" + id +
                ", version='" + getVersion() + '\'' +
                ", identity='" + System.identityHashCode(this) + '\'' +
                ", jobSignature='" + getJobSignature() + '\'' +
                ", jobName='" + getJobName() + '\'' +
                ", jobState='" + getState() + '\'' +
                ", updatedAt='" + getUpdatedAt() + '\'' +
                '}';
    }
}
