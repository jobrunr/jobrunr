package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class JobContext {

    public static final JobContext Null = new JobContext(null);

    private final Job job;

    private JobDashboardLogger jobDashboardLogger;
    private JobDashboardProgressBar jobDashboardProgressBar;

    protected JobContext() {
        // Needed for JSON-B deserialization
        this(null);
    }

    /**
     * Keep constructor package protected to remove confusion on how to instantiate the JobContext.
     * Tip - To use the JobContext in your Job, pass JobContext.Null
     *
     * @param job the job for this JobContext
     */
    protected JobContext(Job job) {
        this.job = job;
    }

    public UUID getJobId() {
        return job.getId();
    }

    public String getJobName() {
        return job.getJobName();
    }

    public StateName getJobState() {
        return job.getState();
    }

    public Instant getCreatedAt() {
        return job.getCreatedAt();
    }

    public Instant getUpdatedAt() {
        return job.getUpdatedAt();
    }

    public String getSignature() {
        return job.getJobSignature();
    }

    public JobDashboardLogger logger() {
        if (jobDashboardLogger == null) {
            jobDashboardLogger = new JobDashboardLogger(job);
        }
        return jobDashboardLogger;
    }

    public JobDashboardProgressBar progressBar(int totalAmount) {
        return progressBar((long) totalAmount);
    }

    public JobDashboardProgressBar progressBar(long totalAmount) {
        if (jobDashboardProgressBar == null) {
            jobDashboardProgressBar = new JobDashboardProgressBar(job, totalAmount);
        }
        return jobDashboardProgressBar;
    }

    public Map<String, Object> getMetadata() {
        return job.getMetadata();
    }

    // marker interface for Jackson Serialization
    public interface Metadata {

    }
}
