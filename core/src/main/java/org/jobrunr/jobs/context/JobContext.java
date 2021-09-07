package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.jobrunr.jobs.context.JobDashboardLogger.JOBRUNR_LOG_KEY;
import static org.jobrunr.jobs.context.JobDashboardProgressBar.JOBRUNR_PROGRESSBAR_KEY;

/**
 * The JobContext class gives access to the Job id, the Job name, the state, ... .
 * <p>
 * It also allows to store some data between different job retries.
 */
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

    /**
     * Gives access to Job Metadata via an UnmodifiableMap. To save Metadata, use the {@link #saveMetadata(String, Object)} method
     *
     * @return all user defined metadata about a Job.
     */
    public Map<String, Object> getMetadata() {
        return unmodifiableMap(
                job.getMetadata().entrySet().stream()
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_LOG_KEY))
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_PROGRESSBAR_KEY))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }

    /**
     * Allows saving metadata for a certain Job. The value must either be a simple type (String, UUID, Integers, ...) or implement the Metadata interface for serialization to Json.
     * Note that it is important that the objects you save are <b>thread-safe</b> (e.g. a CopyOnWriteArrayList, ... ).
     * <p>
     * If the key already exists, the metadata is updated.
     *
     * @param key      the key to store the metadata
     * @param metadata the metadata itself
     */
    public void saveMetadata(String key, Object metadata) {
        validateMetadata(metadata);
        job.getMetadata().put(key, metadata);
    }

    /**
     * Allows saving metadata for a certain Job. The value must either be a simple type (String, UUID, Integers, ...) or implement the Metadata interface for serialization to Json.
     * Note that it is important that the objects you save are <b>thread-safe</b> (e.g. a CopyOnWriteArrayList, ... ).
     * <p>
     * If the key already exists, the metadata is NOT updated.
     *
     * @param key      the key to store the metadata
     * @param metadata the metadata itself
     */
    public void saveMetadataIfAbsent(String key, Object metadata) {
        validateMetadata(metadata);
        job.getMetadata().putIfAbsent(key, metadata);
    }

    private static void validateMetadata(Object metadata) {
        if (!(metadata.getClass().getName().startsWith("java.") || metadata instanceof Metadata)) {
            throw new IllegalArgumentException("All job metadata must either be a simple type (String, UUID, Integers, ...) or implement the Metadata interface for serialization to Json.");
        }
    }

    // marker interface for Json Serialization
    public interface Metadata {

    }
}
