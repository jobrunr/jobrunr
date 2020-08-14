package org.jobrunr.jobs;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class RecurringJobAssert extends AbstractAssert<RecurringJobAssert, RecurringJob> {

    private RecurringJobAssert(RecurringJob recurringJob) {
        super(recurringJob, RecurringJobAssert.class);
    }

    public static RecurringJobAssert assertThat(RecurringJob recurringJob) {
        return new RecurringJobAssert(recurringJob);
    }

    public RecurringJobAssert isEqualTo(RecurringJob otherRecurringJob) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("locker")
                .isEqualTo(otherRecurringJob);
        return this;
    }
}