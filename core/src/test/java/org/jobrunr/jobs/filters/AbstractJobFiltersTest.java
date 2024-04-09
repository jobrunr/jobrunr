package org.jobrunr.jobs.filters;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;

import static java.util.Collections.emptyMap;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class AbstractJobFiltersTest {

    @Test
    void ifJobFilterIsTooSlowAMessageIsLogged() {
        MyJobFilter myJobFilter = new MyJobFilter();
        JobCreationFilters jobCreationFilters = new JobCreationFilters(anEnqueuedJob().build(), new JobDefaultFilters(myJobFilter));
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(jobCreationFilters);

        jobCreationFilters.logJobFilterTime(myJobFilter, 11000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 11000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 11000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 11000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 11000000);

        assertThat(logger).hasWarningMessageContaining(
                "JobFilter of type 'org.jobrunr.jobs.filters.AbstractJobFiltersTest$MyJobFilter' has slow performance (> 10ms) which negatively impacts the overall functioning of JobRunr",
                5,
                emptyMap()
        );
    }

    @Test
    void ifJobFilterIsNotSlowNoMessageIsLogged() {
        MyJobFilter myJobFilter = new MyJobFilter();
        JobCreationFilters jobCreationFilters = new JobCreationFilters(anEnqueuedJob().build(), new JobDefaultFilters(myJobFilter));
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(jobCreationFilters);

        jobCreationFilters.logJobFilterTime(myJobFilter, 1000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 1000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 1000000);

        assertThat(jobCreationFilters.jobFilters())
                .hasAtLeastOneElementOfType(MyJobFilter.class);

        assertThat(logger).hasNoWarnLogMessages();
    }

    public static class MyJobFilter implements JobServerFilter {

    }
}