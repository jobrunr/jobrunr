package org.jobrunr.jobs.filters;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

class AbstractJobFiltersTest {

    @AfterEach
    void resetSlowFilters() {
        ((Map)getInternalState(AbstractJobFilters.class, "slowJobFilters")).clear();
    }

    @Test
    void ifJobFilterIsTooSlowItIsExpelledAndMessageIsLogged() {
        MyJobFilter myJobFilter = new MyJobFilter();
        JobCreationFilters jobCreationFilters = new JobCreationFilters(anEnqueuedJob().build(), new JobDefaultFilters(myJobFilter));
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(jobCreationFilters);

        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);

        assertThat(jobCreationFilters.jobFilters()).doesNotHaveAnyElementsOfTypes(MyJobFilter.class);
        assertThat(logger).hasWarningMessageContaining("JobFilter of type 'org.jobrunr.jobs.filters.AbstractJobFiltersTest$MyJobFilter' is skipped because its slow performance (> 5ms) negatively impacts the overall functioning of JobRunr");
    }

    @Test
    void ifJobFilterIsTemporarySlowButThenFastAgainItIsNotExpelled() {
        MyJobFilter myJobFilter = new MyJobFilter();
        JobCreationFilters jobCreationFilters = new JobCreationFilters(anEnqueuedJob().build(), new JobDefaultFilters(myJobFilter));

        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 1000000);
        jobCreationFilters.logJobFilterTime(myJobFilter, 6000000);

        assertThat(jobCreationFilters.jobFilters())
                .hasAtLeastOneElementOfType(MyJobFilter.class);
    }

    public static class MyJobFilter implements JobServerFilter {

    }
}