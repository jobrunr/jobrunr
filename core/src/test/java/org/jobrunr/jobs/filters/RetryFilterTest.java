package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;

class RetryFilterTest {

    private RetryFilter retryFilter;

    @BeforeEach
    void setupRetryFilter() {
        retryFilter = new RetryFilter();
    }

    @Test
    void skipsIfStateIsNotFailed() {
        final Job job = anEnqueuedJob().build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(ENQUEUED);
    }

    @Test
    void retryFilterSchedulesJobAgainIfStateIsFailed() {
        final Job job = aFailedJob().build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }

    @Test
    void retryFilterSchedulesJobAgainIfStateIsFailedButMaxNumberOfRetriesIsNotReached() {
        final Job job = aJob()
                .<TestService>withJobDetails(ts -> ts.doWorkThatFails())
                .withState(new FailedState("a message", new RuntimeException("boem")))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }

    @Test
    void retryFilterDoesNotScheduleJobAgainIfJobIsJobNotFoundException() {
        final Job job = aJob()
                .withJobDetails(classThatDoesNotExistJobDetails())
                .withState(new FailedState("a message", new JobClassNotFoundException(classThatDoesNotExistJobDetails().build())))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }


    @Test
    void retryFilterDoesNotScheduleJobAgainIfMaxNumberOfRetriesIsReached() {
        final Job job = aJob()
                .<TestService>withJobDetails(ts -> ts.doWorkThatFails())
                .withState(new FailedState("a message", new RuntimeException("boom")))
                .withState(new FailedState("firstRetry", new RuntimeException("boom")))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterDoesNotScheduleJobAgainIfTheExceptionIsProblematic() {
        final Job job = aFailedJob().withState(new FailedState("a message", problematicConfigurationException("big problem"))).build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterDoesNotScheduleJobAgainIfItHasFailed10Times() {
        final Job job = aFailedJobWithRetries().build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterKeepsDefaultRetryFilterValueOf10IfRetriesOnJobAnnotationIsNotProvided() {
        final Job job = aJob()
                .<TestService>withJobDetails(ts -> ts.doWork())
                .withState(new FailedState("a message", new RuntimeException("boom")))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);
    }

    @Test
    void retryFilterKeepsDefaultGivenRetryFilterValueIfRetriesOnJobAnnotationIsNotProvided() {
        retryFilter = new RetryFilter(0);
        final Job job = aJob()
                .<TestService>withJobDetails(ts -> ts.doWork())
                .withState(new FailedState("a message", new RuntimeException("boom")))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        retryFilter.onStateElection(job, job.getJobState());
        int afterVersion = job.getJobStates().size();

        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    @Test
    void retryFilterUsesValueOfRetriesOnJobAnnotationIfProvided() {
        retryFilter = new RetryFilter(0);

        // GIVEN FIRST FAILURE, NOT YET RETRIED
        Job job = aJob()
                .<TestService>withJobDetails(ts -> ts.doWorkThatFails())
                .withState(new FailedState("a message", new RuntimeException("boom")))
                .build();
        applyDefaultJobFilter(job);
        int beforeVersion = job.getJobStates().size();

        // WHEN
        retryFilter.onStateElection(job, job.getJobState());

        // THEN
        int afterVersion = job.getJobStates().size();
        assertThat(afterVersion).isEqualTo(beforeVersion + 1);
        assertThat(job.getState()).isEqualTo(SCHEDULED);

        // GIVEN SECOND FAILURE, ALREADY RETRIED
        job = aCopyOf(job)
                .withState(new FailedState("a message", new RuntimeException("boom")))
                .build();
        beforeVersion = job.getJobStates().size();

        // WHEN
        retryFilter.onStateElection(job, job.getJobState());

        // THEN
        afterVersion = job.getJobStates().size();
        assertThat(afterVersion).isEqualTo(beforeVersion);
        assertThat(job.getState()).isEqualTo(FAILED);
    }

    void applyDefaultJobFilter(Job job) {
        new DefaultJobFilter().onCreating(job);
    }
}