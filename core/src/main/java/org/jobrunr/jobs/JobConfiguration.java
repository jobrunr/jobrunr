package org.jobrunr.jobs;

import java.time.Duration;

/**
 * This class allows to configure everything related to {@link Job Jobs}
 */
public class JobConfiguration {

    public static final Duration DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION = Duration.ofHours(36);
    public static final Duration DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION = Duration.ofHours(72);

    Duration deleteSucceededJobsAfter = DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION;
    Duration permanentlyDeleteDeletedJobsAfter = DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION;

    private JobConfiguration() {

    }

    /**
     * This returns the default configuration with the BackgroundJobServer with a poll interval of 15 seconds and a worker count based on the CPU
     *
     * @return the default JobRunrDashboard configuration
     */
    public static JobConfiguration usingStandardJobConfiguration() {
        return new JobConfiguration();
    }

    /**
     * Allows to set the duration to wait before deleting succeeded jobs
     *
     * @param duration the duration to wait before deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public JobConfiguration andDeleteSucceededJobsAfter(Duration duration) {
        this.deleteSucceededJobsAfter = duration;
        return this;
    }

    /**
     * Allows to set the duration to wait before permanently deleting succeeded jobs
     *
     * @param duration the duration to wait before permanently deleting successful jobs
     * @return the same configuration instance which provides a fluent api
     */
    public JobConfiguration andPermanentlyDeleteDeletedJobsAfter(Duration duration) {
        this.permanentlyDeleteDeletedJobsAfter = duration;
        return this;
    }
}
