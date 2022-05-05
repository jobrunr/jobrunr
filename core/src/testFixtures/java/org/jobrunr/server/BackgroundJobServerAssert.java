package org.jobrunr.server;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.RetryFilter;

import java.util.List;

import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

public class BackgroundJobServerAssert extends AbstractAssert<BackgroundJobServerAssert, BackgroundJobServer> {
    protected BackgroundJobServerAssert(BackgroundJobServer backgroundJobServe) {
        super(backgroundJobServe, BackgroundJobServerAssert.class);
    }

    public static BackgroundJobServerAssert assertThat(BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerAssert(backgroundJobServer);
    }

    public BackgroundJobServerAssert hasRetryFilter(int defaultNbrOfRetries) {
        List<JobFilter> filters = getInternalState(actual.getJobFilters(), "filters");
        JobFilter retryFilter = filters.stream()
                .filter(filter -> filter instanceof RetryFilter)
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No retry filter found"));
        Assertions.assertThat((Integer) getInternalState(retryFilter, "numberOfRetries")).isEqualTo(defaultNbrOfRetries);
        return this;
    }

    public BackgroundJobServerAssert hasJobFilterOfType(Class<? extends JobFilter> jobFilterClass) {
        List<JobFilter> filters = getInternalState(actual.getJobFilters(), "filters");
        Assertions.assertThat(filters).anyMatch(jobFilter -> jobFilterClass.equals(jobFilter.getClass()));
        return this;
    }
}
