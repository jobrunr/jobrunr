package org.jobrunr.jobs;

import org.jobrunr.jobs.states.DeletedState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

/**
 * Defines the job with it's JobDetails, History and Job Metadata
 */
public class Job extends AbstractJob {

    private UUID id;
    private ArrayList<JobState> jobHistory;
    private final Map<String, Object> metadata;

    private Job() {
        // used for deserialization
        this.metadata = new ConcurrentHashMap<>();
    }

    public Job(JobDetails jobDetails) {
        this(jobDetails, new EnqueuedState());
    }

    public Job(JobDetails jobDetails, JobState jobState) {
        this(jobDetails, singletonList(jobState));
    }

    public Job(JobDetails jobDetails, List<JobState> jobHistory) {
        this(null, 0, jobDetails, jobHistory, new ConcurrentHashMap<>());
    }

    public Job(UUID id, int version, JobDetails jobDetails, List<JobState> jobHistory, ConcurrentHashMap<String, Object> metadata) {
        super(jobDetails, version);
        if (jobHistory.isEmpty()) throw new IllegalStateException("A job should have at least one initial state");
        this.id = id;
        this.jobHistory = new ArrayList<>(jobHistory);
        this.metadata = metadata;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public List<JobState> getJobStates() {
        return unmodifiableList(jobHistory);
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

    protected void addJobState(JobState jobState) {
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
        addJobState(new SucceededState(Duration.between(getCreatedAt(), Instant.now()), Duration.between(getUpdatedAt(), Instant.now())));
    }

    public void failed(String message, Exception exception) {
        addJobState(new FailedState(message, exception));
    }

    public void delete() {
        addJobState(new DeletedState());
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
                ", jobSignature='" + getJobSignature() + '\'' +
                ", jobName='" + getJobName() + '\'' +
                ", updatedAt='" + getUpdatedAt() + '\'' +
                '}';
    }
}
