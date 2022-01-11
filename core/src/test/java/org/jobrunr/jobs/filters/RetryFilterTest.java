package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.jobs.states.StateName.*;

class RetryFilterTest {

    private RetryFilter retryFilter;

    @BeforeEach
    void setupRetryFilter() {
        retryFilter = new RetryFilter();
    }

    @Test
    void skipsIfTestIsNotFailed() {
        final Job job = anEnqueuedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(ENQUEUED);
    }

    @Test
    void retryFilterSchedulesJobAgainIfItIsFailed() {
        final Job job = aFailedJob().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }

    @Test
    void retryFilterSchedulesJobAgainIfItIsFailedButMaxNumberOfRetriesIsNotReached() {
        final Job job = aJob()
                .withJobDetails((IocJobLambda<TestService>) (ts -> ts.doWorkThatFails()))
                .withState(new FailedState("a message", new RuntimeException("boem")))
                .build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }


    @Test
    void retryFilterDoesNotScheduleJobAgainIfMaxNumberOfRetriesIsReached() {
        final Job job = aJob()
                .withJobDetails((IocJobLambda<TestService>) (ts -> ts.doWorkThatFails()))
                .withState(new FailedState("a message", new RuntimeException("boem")))
                .withState(new FailedState("firstRetry", new RuntimeException("boem")))
                .build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterDoesNotScheduleJobAgainIfTheExceptionIsProblematic() {
        final Job job = aFailedJob().withState(new FailedState("a message", problematicConfigurationException("big problem"))).build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterDoesNotScheduleJobAgainIfItHasFailed10Times() {
        final Job job = aFailedJobWithRetries().build();
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }
}