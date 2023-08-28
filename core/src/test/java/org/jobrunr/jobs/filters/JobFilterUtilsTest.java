package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class JobFilterUtilsTest {

    private LogAllStateChangesFilter logAllStateChangesFilter;
    private JobFilterUtils jobFilterUtils;

    @BeforeEach
    void setUpJobFilterUtils() {
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters(logAllStateChangesFilter);
        jobFilterUtils = new JobFilterUtils(jobDefaultFilters);
    }

    @Test
    void ifExecuteJobServerFilterIsTrueOnStateAppliedFilterIsInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobFilterUtils.runOnStateAppliedFilters(List.of(aJobInProgress), true);

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).containsExactly("ENQUEUED->PROCESSING");
    }

    @Test
    void ifExecuteJobServerFilterIsFalseOnStateAppliedFilterIsNotInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        jobFilterUtils.runOnStateAppliedFilters(List.of(aJobInProgress), false);

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).isEmpty();
    }
}