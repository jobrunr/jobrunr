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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

/**
 * Defines the job with it's JobDetails, History and Job Metadata
 */
public class Job extends AbstractJob {

    private UUID id;
    private ArrayList<JobState> jobHistory;
    private volatile Map<String, Object> metadata;

    private Job() {
        // used for deserialization
    }

    public Job(JobDetails jobDetails) {
        this(jobDetails, new EnqueuedState());
    }

    public Job(JobDetails jobDetails, JobState jobState) {
        this(jobDetails, singletonList(jobState));
    }

    public Job(JobDetails jobDetails, List<JobState> jobHistory) {
        super(jobDetails);
        if (jobHistory.isEmpty()) throw new IllegalStateException("A job should have at least one initial state");
        this.jobHistory = new ArrayList<>(jobHistory);
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

    public boolean hasState(StateName state) {
        return getState().equals(state);
    }

    public Instant getCreatedAt() {
        return getJobState(0).getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return getJobState().getUpdatedAt();
    }

    Map<String, Object> getMetadata() {
        if (metadata == null) metadata = new HashMap<>();
        return metadata;
    }

    public void enqueue() {
        this.jobHistory.add(new EnqueuedState());
    }

    public void scheduleAt(Instant instant, String reason) {
        this.jobHistory.add(new ScheduledState(instant, reason));
    }

    public void startProcessingOn(BackgroundJobServer backgroundJobServer) {
        if (getState() == StateName.PROCESSING) throw new ConcurrentJobModificationException(this);
        this.jobHistory.add(new ProcessingState(backgroundJobServer.getId()));
    }

    public void updateProcessing() {
        ProcessingState jobState = getJobState();
        jobState.setUpdatedAt(Instant.now());
    }

    public void succeeded() {
        this.jobHistory.add(new SucceededState(Duration.between(getCreatedAt(), Instant.now()), Duration.between(getUpdatedAt(), Instant.now())));
    }

    public void failed(String message, Exception exception) {
        this.jobHistory.add(new FailedState(message, exception));
    }

    public void delete() {
        this.jobHistory.add(new DeletedState());
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
