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

class JobFiltersTest {

    private JobFilters jobFilters;

    private TestService testService;

    @BeforeEach
    void setUp() {
        jobFilters = new JobFilters();
        testService = new TestService();
    }

    @Test
    void ifNoElectStateFilterIsProvidedTheDefaultRetryFilterIsUsed() {
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateElectionFilter(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }

    @Test
    void ifElectStateFilterIsProvidedItIsUsed() {
        Job aJobWithACustomElectStateJobFilter = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCustomJobFilters()).build();
        jobFilters.runOnStateElectionFilter(aJobWithACustomElectStateJobFilter);
        assertThat(aJobWithACustomElectStateJobFilter.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, SUCCEEDED);
    }

    @Test
    void ifADefaultElectStateFilterIsProvidedItIsUsed() {
        jobFilters = new JobFilters(new TestService.TheSunIsAlwaysShiningElectStateFilter());
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateElectionFilter(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, SUCCEEDED);
    }

    @Test
    void ifOtherFilterIsProvidedItIsUsed() {
        Job aJobWithACustomElectStateJobFilter = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCustomJobFilters()).build();
        jobFilters.runOnStateAppliedFilters(aJobWithACustomElectStateJobFilter);
        jobFilters.runOnJobProcessingFilters(aJobWithACustomElectStateJobFilter);
        jobFilters.runOnJobProcessedFilters(aJobWithACustomElectStateJobFilter);
        Map<String, Object> metadata = Whitebox.getInternalState(aJobWithACustomElectStateJobFilter, "metadata");

        assertThat(metadata)
                .containsKey("onStateApplied")
                .containsKey("onProcessing")
                .containsKey("onProcessed");
    }

    @Test
    void exceptionsAreCatched() {
        jobFilters = new JobFilters(new JobFilterThatThrowsAnException());

        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateAppliedFilters(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void noExceptionIsThrownIfJobClassIsNotFound() {
        Job aJobClassThatDoesNotExist = anEnqueuedJob().withJobDetails(classThatDoesNotExistJobDetails()).build();
        assertThatCode(() -> jobFilters.runOnStateElectionFilter(aJobClassThatDoesNotExist)).doesNotThrowAnyException();
        assertThatCode(() -> jobFilters.runOnStateAppliedFilters(aJobClassThatDoesNotExist)).doesNotThrowAnyException();
    }

    @Test
    void noExceptionIsThrownIfJobMethodIsNotFound() {
        Job aJobMethodThatDoesNotExist = anEnqueuedJob().withJobDetails(methodThatDoesNotExistJobDetails()).build();
        assertThatCode(() -> jobFilters.runOnStateElectionFilter(aJobMethodThatDoesNotExist)).doesNotThrowAnyException();
        assertThatCode(() -> jobFilters.runOnStateAppliedFilters(aJobMethodThatDoesNotExist)).doesNotThrowAnyException();
    }

    private static class JobFilterThatThrowsAnException implements ApplyStateFilter {

        @Override
        public void onStateApplied(Job job, JobState oldState, JobState newState) {
            throw new RuntimeException("boem!");
        }
    }

}