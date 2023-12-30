package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJob;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JobPerformingFiltersTest {

    private TestService testService;

    @Mock
    BackgroundJobServer backgroundJobServer;

    @BeforeEach
    void setUp() {
        testService = new TestService();

        lenient().when(backgroundJobServer.getConfiguration()).thenReturn(usingStandardBackgroundJobServerConfiguration());
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
        aJobWithACustomElectStateJobFilter.startProcessingOn(backgroundJobServer);
        JobPerformingFilters jobPerformingFilters = jobPerformingFilters(aJobWithACustomElectStateJobFilter);
        jobPerformingFilters.runOnStateAppliedFilters();
        jobPerformingFilters.runOnJobProcessingFilters();
        jobPerformingFilters.runOnJobProcessingSucceededFilters();
        Map<String, Object> metadata = Whitebox.getInternalState(aJobWithACustomElectStateJobFilter, "metadata");

        assertThat(metadata)
                .containsKey("onStateApplied")
                .containsKey("onProcessing");
    }

    @Test
    void ifOtherFilterUsesDependencyInjectionThisWillThrowRuntimeException() {
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters();

        Job aJobWithoutJobFilters = aJobInProgress().withJobDetails(() -> testService.doWorkWithCustomJobFilterThatNeedsDependencyInjection()).build();

        assertThatThrownBy(() -> jobPerformingFilters(aJobWithoutJobFilters, jobDefaultFilters).runOnJobProcessingFilters())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Do you want to use JobFilter Beans? This is only possible in the Pro version. Check out https://www.jobrunr.io/en/documentation/pro/job-filters/");
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

    @Test
    void testAllFilters() {
        Job job = anEnqueuedJob().build();

        LogAllStateChangesFilter logAllStateChangesFilter = new LogAllStateChangesFilter();
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(logAllStateChangesFilter);

        job.startProcessingOn(backgroundJobServer);
        jobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();
        jobPerformingFilters(job, jobDefaultFilters).runOnJobProcessingFilters();

        job.failed("Exception occurred", new RuntimeException());
        jobPerformingFilters(job, jobDefaultFilters).runOnJobProcessingFailedFilters(new RuntimeException());
        jobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();

        job.enqueue();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();

        job.startProcessingOn(backgroundJobServer);
        jobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();
        jobPerformingFilters(job, jobDefaultFilters).runOnJobProcessingFilters();

        job.succeeded();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateElectionFilter();
        jobPerformingFilters(job, jobDefaultFilters).runOnStateAppliedFilters();
        jobPerformingFilters(job, jobDefaultFilters).runOnJobProcessingSucceededFilters();

        assertThat(logAllStateChangesFilter.getStateChanges(job))
                .containsExactly(
                        "ENQUEUED->PROCESSING",
                        "PROCESSING->FAILED",
                        "FAILED->SCHEDULED",
                        "SCHEDULED->ENQUEUED",
                        "ENQUEUED->PROCESSING",
                        "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingFailedIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isTrue();
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