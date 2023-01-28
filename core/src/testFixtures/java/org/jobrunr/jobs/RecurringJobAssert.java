package org.jobrunr.jobs;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.JobRunrAssertions;

import java.util.Set;

public class RecurringJobAssert extends AbstractAssert<RecurringJobAssert, RecurringJob> {

    private RecurringJobAssert(RecurringJob recurringJob) {
        super(recurringJob, RecurringJobAssert.class);
    }

    public static RecurringJobAssert assertThat(RecurringJob recurringJob) {
        return new RecurringJobAssert(recurringJob);
    }

    public RecurringJobAssert hasId() {
        Assertions.assertThat(actual.getId()).isNotNull();
        return this;
    }

    public RecurringJobAssert hasId(String id) {
        Assertions.assertThat(actual.getId()).isEqualTo(id);
        return this;
    }

    public RecurringJobAssert hasJobName(String name) {
        Assertions.assertThat(actual.getJobName()).isEqualTo(name);
        return this;
    }

    public RecurringJobAssert hasJobDetails(Class<?> clazz, String methodName, Object... args) {
        JobRunrAssertions.assertThat(actual.getJobDetails())
                .hasClass(clazz)
                .hasMethodName(methodName)
                .hasArgs(args);
        return this;
    }

    public RecurringJobAssert hasRetries(int amountOfRetries) {
        Assertions.assertThat(actual.getAmountOfRetries()).isEqualTo(amountOfRetries);
        return this;
    }

    public RecurringJobAssert hasLabels(Set<String> labels) {
        Assertions.assertThat(actual.getLabels()).isEqualTo(labels);
        return this;
    }

    public RecurringJobAssert hasScheduleExpression(String scheduleExpression) {
        Assertions.assertThat(actual.getScheduleExpression()).isEqualTo(scheduleExpression);
        return this;
    }

    public RecurringJobAssert hasZoneId(String zoneId) {
        Assertions.assertThat(actual.getZoneId()).isEqualTo(zoneId);
        return this;
    }

    public RecurringJobAssert isEqualTo(RecurringJob otherRecurringJob) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFields("locker")
                .isEqualTo(otherRecurringJob);
        return this;
    }
}