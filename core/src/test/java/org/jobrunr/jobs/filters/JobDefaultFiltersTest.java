package org.jobrunr.jobs.filters;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobDefaultFiltersTest {

    @Test
    void jobDefaultFiltersHasDefaultJobFilterAndRetryFilter() {
        JobDefaultFilters jobDefaultFilters = new JobDefaultFilters();
        assertThat(jobDefaultFilters.getFilters())
                .hasAtLeastOneElementOfType(DefaultJobFilter.class)
                .hasAtLeastOneElementOfType(RetryFilter.class);
    }
}