package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

class JobFiltersTest {

    private JobFilters jobFilters;

    private TestService testService;

    @BeforeEach
    void setUp() {
        jobFilters = new JobFilters();
        testService = new TestService();
    }

    @Test
    public void ifNoElectStateFilterIsProvidedTheDefaultRetryFilterIsUsed() {
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateElectionFilter(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, SCHEDULED);
    }

    @Test
    public void ifElectStateFilterIsProvidedItIsUsed() {
        Job aJobWithACustomElectStateJobFilter = anEnqueuedJob().withJobDetails(() -> testService.doWorkWithCustomJobFilters()).build();
        jobFilters.runOnStateElectionFilter(aJobWithACustomElectStateJobFilter);
        assertThat(aJobWithACustomElectStateJobFilter.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, SUCCEEDED);
    }

    @Test
    public void ifADefaultElectStateFilterIsProvidedItIsUsed() {
        jobFilters = new JobFilters(new TestService.TheSunIsAlwaysShiningElectStateFilter());
        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateElectionFilter(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED, SUCCEEDED);
    }

    @Test
    public void ifOtherFilterIsProvidedItIsUsed() {
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
    public void exceptionsAreCatched() {
        jobFilters = new JobFilters(new JobFilterThatThrowsAnException());

        Job aJobWithoutJobFilters = aFailedJob().build();
        jobFilters.runOnStateAppliedFilters(aJobWithoutJobFilters);
        assertThat(aJobWithoutJobFilters.getJobStates())
                .extracting("state")
                .containsExactly(ENQUEUED, PROCESSING, FAILED);
    }

    private static class JobFilterThatThrowsAnException implements ApplyStateFilter {

        @Override
        public void onStateApplied(Job job, JobState oldState, JobState newState) {
            throw new RuntimeException("boem!");
        }
    }

}