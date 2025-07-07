package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static org.jobrunr.jobs.context.JobDashboardLogger.JOBRUNR_LOG_KEY;
import static org.jobrunr.jobs.context.JobDashboardProgressBar.JOBRUNR_PROGRESSBAR_KEY;
import static org.jobrunr.jobs.mappers.MDCMapper.JOBRUNR_MDC_KEY;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

/**
 * The JobContext class gives access to the Job id, the Job name, the state, ... .
 * <p>
 * Using the {@link #getMetadata()}, it also allows to store some data between different job retries so jobs can be made re-entrant.
 * This comes in handy when your job exists out of multiple steps, and you want to keep track of which step already succeeded. Then,
 * in case of a failure, you can skip the steps that already completed successfully.
 * As soon as the job is completed successfully the metadata is cleared (for storage purpose reasons).
 */
public class JobContext {

    private static final String JOBRUNR_STEP_PREFIX = "jr_step_";

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

    public List<String> getJobLabels() {
        return job.getLabels();
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

    public String getJobSignature() {
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
     * @return all user defined metadata about a Job. This metadata is only accessible up to the point a job succeeds.
     */
    public Map<String, Object> getMetadata() {
        return unmodifiableMap(
                job.getMetadata().entrySet().stream()
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_LOG_KEY))
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_PROGRESSBAR_KEY))
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_MDC_KEY))
                        .filter(entry -> !entry.getKey().startsWith(JOBRUNR_STEP_PREFIX))
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

    /**
     * Allows retrieving the metadata with the given key from a Job.
     * <p>
     *
     * @param key the key to retrieve the metadata
     * @return the given value associated with the provided key.
     */
    public <T> T getMetadata(String key) {
        return cast(job.getMetadata().get(key));
    }

    /**
     * Returns true if the given step has already completed successfully in a previous run.
     */
    public boolean hasCompletedStep(String stepName) {
        Object value = getMetadata(JOBRUNR_STEP_PREFIX + stepName);
        if (value == null) return false;
        if (value instanceof Boolean) return (boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        throw new IllegalStateException("Unsupported step value: " + stepName);
    }

    /**
     * Marks the given step as completed (so it won’t run again if a job retries due to an exception).
     */
    void markStepCompleted(String stepName) {
        saveMetadata(JOBRUNR_STEP_PREFIX + stepName, true);
    }

    /**
     * Run the supplied task exactly once (i.e. only if it hasn’t already completed).
     * If the task throws, the step won’t be marked completed.
     */
    public void runStepOnce(String step, Runnable task) {
        if (!hasCompletedStep(step)) {
            task.run();
            markStepCompleted(step);
        }
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
