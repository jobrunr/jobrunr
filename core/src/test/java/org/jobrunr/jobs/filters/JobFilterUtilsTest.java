package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobTestFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class JobFilterUtilsTest {

    private BackgroundJobTestFilter backgroundJobTestFilter;
    private JobFilterUtils jobFilterUtils;

    @BeforeEach
    void setUpJobFilterUtils() {
        backgroundJobTestFilter = new BackgroundJobTestFilter();
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(backgroundJobTestFilter);
        jobFilterUtils = new JobFilterUtils(jobDefaultFilters);
    }

    @Test
    void ifExecuteJobServerFilterIsTrueOnStateAppliedFilterIsInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobFilterUtils.runOnStateAppliedFilters(List.of(aJobInProgress), true);

        assertThat(backgroundJobTestFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING");
    }

    @Test
    void ifExecuteJobServerFilterIsFalseOnStateAppliedFilterIsNotInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobFilterUtils.runOnStateAppliedFilters(List.of(aJobInProgress), false);

        assertThat(backgroundJobTestFilter.stateChanges).isEmpty();
    }
}