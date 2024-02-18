package org.jobrunr.jobs;

import java.time.Duration;

public class JobConfigurationReader {

    private final JobConfiguration configuration;

    public JobConfigurationReader(JobConfiguration configuration) {
        this.configuration = configuration;
    }

    public Duration getDeleteSucceededJobsAfter() {
        return configuration.deleteSucceededJobsAfter;
    }

    public Duration getPermanentlyDeleteDeletedJobsAfter() {
        return configuration.permanentlyDeleteDeletedJobsAfter;
    }
}
