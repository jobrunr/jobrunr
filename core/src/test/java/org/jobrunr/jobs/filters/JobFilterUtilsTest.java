package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobFilterUtilsTest {

    private LogAllStateChangesFilter logAllStateChangesFilter;
    private JobFilterUtils jobFilterUtils;

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    void setUpJobFilterUtils() {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(logAllStateChangesFilter);
        jobFilterUtils = new JobFilterUtils(jobDefaultFilters);

        when(backgroundJobServer.getConfiguration()).thenReturn(usingStandardBackgroundJobServerConfiguration());
    }

    @Test
    void jobFiltersAreExecutedIfJobHasStateChange() {
        // GIVEN
        Job aJob = anEnqueuedJob().build();
        aJob.getStateChangesForJobFilters(); // clear

        // WHEN
        aJob.startProcessingOn(backgroundJobServer);
        jobFilterUtils.runOnStateAppliedFilters(List.of(aJob));

        // THEN
        assertThat(logAllStateChangesFilter.getStateChanges(aJob)).containsExactly("ENQUEUED->PROCESSING");
    }

    @Test
    void jobFiltersAreNotAppliedIfJobHasNoStateChange() {
        // GIVEN
        Job aJob = anEnqueuedJob().build();
        aJob.startProcessingOn(backgroundJobServer);
        aJob.getStateChangesForJobFilters();  // clear

        // WHEN
        aJob.updateProcessing();
        jobFilterUtils.runOnStateAppliedFilters(List.of(aJob));

        // THEN
        assertThat(logAllStateChangesFilter.getStateChanges(aJob)).isEmpty();
    }
}