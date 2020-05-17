package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.FailedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;

class RetryFilterTest {

    private RetryFilter retryFilter;

    @BeforeEach
    public void setupRetryFilter() {
        retryFilter = new RetryFilter();
    }

    @Test
    public void skipsIfTestIsNotFailed() {
        final Job job = anEnqueuedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(ENQUEUED);
    }

    @Test
    public void enqueuesJobAgainIfItIsFailed() {
        final Job job = aFailedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }

    @Test
    public void doesNotEnqueuesJobAgainIfItExceptionIsProblematic() {
        final Job job = aFailedJob().withState(new FailedState("a message", problematicConfigurationException("big problem"))).build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    public void doesNotEnqueueJobAgainIfItHasFailed10Times() {
        final Job job = aFailedJobWithRetries().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

}