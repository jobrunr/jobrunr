package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class RetryFilterTest {

    private RetryFilter retryFilter;

    @BeforeEach
    public void setupRetryFilter() {
        retryFilter = new RetryFilter();
    }

    @Test
    public void skipsIsTestIsNotFailed() {
        final Job job = anEnqueuedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
    }

    @Test
    public void enqueuesJobAgainIfItIsFailed() {
        final Job job = aFailedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
    }

    @Test
    public void doesNotEnqueueJobAgainIfItHasFailed10Times() {
        final Job job = aFailedJobWithRetries().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
    }

}