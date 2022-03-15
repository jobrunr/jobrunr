package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.*;

class JobPerformingFiltersTest {

    private TestService testService;

    @BeforeEach
    void setUp() {
        testService = new TestService();
    }

    @Test
    void ifNoElectStateFilterIsProvidedTheDefaultRetryFilterIsUsed() {
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobPerformingFilters(aJobWithoutJobFilters).runOnStateElectionFilter();
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }

    @Test
    void ifElectStateFilterIsProvidedItIsUsed() {
        Job aJobWithACustomElectStateJobFilter = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCustomJobFilters()).build();
        jobPerformingFilters(aJobWithACustomElectStateJobFilter).runOnStateElectionFilter();
        assertThat(aJobWithACustomElectStateJobFilter.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, SUCCEEDED);
    }

    @Test
    void ifADefaultElectStateFilterIsProvidedItIsUsed() {
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(new TestService.FailedToDeleteElectStateFilter());
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobPerformingFilters(aJobWithoutJobFilters, jobDefaultFilters).runOnStateElectionFilter();
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, DELETED);
    }

    @Test
    void ifOtherFilterIsProvidedItIsUsed() {
        Job aJobWithACustomElectStateJobFilter = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCustomJobFilters()).build();
        JobPerformingFilters jobPerformingFilters = jobPerformingFilters(aJobWithACustomElectStateJobFilter);
        jobPerformingFilters.runOnStateAppliedFilters();
        jobPerformingFilters.runOnJobProcessingFilters();
        jobPerformingFilters.runOnJobProcessedFilters();
        Map<String, Object> metadata = Whitebox.getInternalState(aJobWithACustomElectStateJobFilter, "metadata");

        assertThat(metadata)
                .containsKey("onStateApplied")
                .containsKey("onProcessing")
                .containsKey("onProcessed");
    }

    @Test
    void exceptionsAreCatched() {
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(new JobFilterThatThrowsAnException());

        Job aJobWithoutJobFilters = aFailedJob().build();
        jobPerformingFilters(aJobWithoutJobFilters, jobDefaultFilters).runOnStateAppliedFilters();
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void noExceptionIsThrownIfJobClassIsNotFound() {
        Job aJobClassThatDoesNotExist = anEnqueuedJob().withJobDetails(classThatDoesNotExistJobDetails()).build();
        assertThatCode(() -> jobPerformingFilters(aJobClassThatDoesNotExist).runOnStateElectionFilter()).doesNotThrowAnyException();
        assertThatCode(() -> jobPerformingFilters(aJobClassThatDoesNotExist).runOnStateAppliedFilters()).doesNotThrowAnyException();
    }

    @Test
    void noExceptionIsThrownIfJobMethodIsNotFound() {
        Job aJobMethodThatDoesNotExist = anEnqueuedJob().withJobDetails(methodThatDoesNotExistJobDetails()).build();
        assertThatCode(() -> jobPerformingFilters(aJobMethodThatDoesNotExist).runOnStateElectionFilter()).doesNotThrowAnyException();
        assertThatCode(() -> jobPerformingFilters(aJobMethodThatDoesNotExist).runOnStateAppliedFilters()).doesNotThrowAnyException();
    }

    private JobPerformingFilters jobPerformingFilters(Job job) {
        return jobPerformingFilters(job, new JobDefaultFilters());
    }

    private JobPerformingFilters jobPerformingFilters(Job job, JobDefaultFilters jobDefaultFilters) {
        return new JobPerformingFilters(job, jobDefaultFilters);
    }

    private static class JobFilterThatThrowsAnException implements ApplyStateFilter {

        @Override
        public void onStateApplied(Job job, JobState oldState, JobState newState) {
            throw new RuntimeException("boem!");
        }
    }

}