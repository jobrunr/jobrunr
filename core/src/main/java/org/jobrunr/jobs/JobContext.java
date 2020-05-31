package org.jobrunr.jobs;

import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class JobContext {

    public static final JobContext Null = new JobContext(null);

    private final Job job;

    private JobContext() {
        this.job = null;
    }

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

    public String getJobIdentifier() {
        return job.getJobSignature();
    }

    public Map<String, Object> getMetadata() {
        return job.getMetadata();
    }

    // marker interface for Jackson Serialization
    public interface Metadata {

    }
}
