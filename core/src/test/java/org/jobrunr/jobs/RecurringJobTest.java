package org.jobrunr.jobs;

import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

class RecurringJobTest {

    @Test
    public void testToScheduledJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob().withName("the recurring job").build();

        final Job job = recurringJob.toScheduledJob();

        assertThat(job).hasJobName("the recurring job");
    }

    @Test
    public void testToEnqueuedJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob().withName("the recurring job").build();

        final Job job = recurringJob.toEnqueuedJob();

        assertThat(job).hasJobName("the recurring job");
    }
}