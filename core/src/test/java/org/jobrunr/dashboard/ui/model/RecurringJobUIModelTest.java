package org.jobrunr.dashboard.ui.model;

import org.jobrunr.jobs.RecurringJob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

class RecurringJobUIModelTest {

    @Test
    void RecurringJobUIModelHasAllFieldsOfRecurringJob() {
        final RecurringJob recurringJob = aDefaultRecurringJob().withId("import-sales-data").withName("Import all sales data at midnight").build();
        final RecurringJobUIModel recurringJobUIModel = new RecurringJobUIModel(recurringJob);

        assertThat(recurringJobUIModel)
                .isEqualToIgnoringGivenFields(recurringJob, "locker", "lastScheduledJobsCheck");
    }

}