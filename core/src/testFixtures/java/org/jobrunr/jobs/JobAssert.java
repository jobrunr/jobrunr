package org.jobrunr.jobs;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.data.TemporalOffset;
import org.jobrunr.JobRunrAssertions;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.context.JobDashboardProgressBar;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.condition.AnyOf.anyOf;

public class JobAssert extends AbstractAssert<JobAssert, Job> {

    private JobAssert(Job job) {
        super(job, JobAssert.class);
    }

    public static JobAssert assertThat(Job job) {
        return new JobAssert(job);
    }

    public JobAssert hasId() {
        Assertions.assertThat(actual.getId()).isNotNull();
        return this;
    }


    public JobAssert hasId(UUID id) {
        Assertions.assertThat(actual.getId())
                .isNotNull()
                .isEqualTo(id);
        return this;
    }

    public JobAssert hasJobName(String name) {
        Assertions.assertThat(actual.getJobName()).isEqualTo(name);
        return this;
    }

    public JobAssert hasJobDetails(Class<?> clazz, String methodName, Object... args) {
        JobRunrAssertions.assertThat(actual.getJobDetails())
                .hasClass(clazz)
                .hasMethodName(methodName)
                .hasArgs(args);
        return this;
    }

    public JobAssert hasUpdatedAtCloseTo(Instant instant, TemporalOffset<Temporal> temporalOffset) {
        Assertions.assertThat(actual.getUpdatedAt()).isCloseTo(instant, temporalOffset);
        return this;
    }

    public JobAssert hasState(StateName state) {
        Assertions.assertThat(actual.getState()).isEqualTo(state);
        return this;
    }

    public JobAssert hasOneOfTheFollowingStates(StateName... states) {
        Assertions.assertThat(actual).has(anyOf(Arrays.stream(states).map(JobStateCondition::new).collect(Collectors.toList())));
        return this;
    }

    public JobAssert doesNotHaveState(StateName stateName) {
        Assertions.assertThat(actual.getState()).isNotEqualTo(stateName);
        return this;
    }

    public JobAssert hasStates(StateName... state) {
        List<StateName> jobStates = actual.getJobStates().stream().map(JobState::getName).collect(Collectors.toList());
        Assertions.assertThat(jobStates).containsExactly(state);
        return this;
    }

    public JobAssert hasMetadata(String key) {
        Assertions.assertThat(actual.getMetadata()).containsKey(key);
        return this;
    }

    public JobAssert hasMetadata(String key, String value) {
        Assertions.assertThat(actual.getMetadata()).containsEntry(key, value);
        return this;
    }

    public JobAssert hasMetadata(Condition condition) {
        Assertions.assertThat(actual.getMetadata()).has(condition);
        return this;
    }

    public JobAssert hasMetadataOnlyContainingJobProgressAndLogging() {
        for (String key : actual.getMetadata().keySet()) {
            if(!(key.startsWith(JobDashboardLogger.JOBRUNR_LOG_KEY) || key.startsWith(JobDashboardProgressBar.JOBRUNR_PROGRESSBAR_KEY))) {
                throw new AssertionError("Job has metadata key '" + key + "' which is not allowed");
            }
        }
        return this;
    }

    public JobAssert hasNoMetadata() {
        Assertions.assertThat(actual.getMetadata()).isEmpty();
        return this;
    }

    public JobAssert hasVersion(int version) {
        Assertions.assertThat(actual.getVersion()).isEqualTo(version);
        return this;
    }

    public JobAssert hasAmountOfRetries(int amountOfRetries) {
        Assertions.assertThat(actual.getAmountOfRetries()).isEqualTo(amountOfRetries);
        return this;
    }

    public JobAssert hasLabels(Set<String> labels) {
        Assertions.assertThat(actual.getLabels()).isEqualTo(labels);
        return this;
    }

    public JobAssert hasRecurringJobId(String recurringJobId) {
        Assertions.assertThat(actual.getRecurringJobId())
                .isPresent()
                .contains(recurringJobId);
        return this;
    }

    public JobAssert isEqualTo(Job otherJob) {
        return isEqualTo(otherJob, "locker");
    }

    public JobAssert isEqualTo(Job otherJob, String... fieldNamesToIgnore) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .usingOverriddenEquals()
                .ignoringFields(fieldNamesToIgnore)
                .isEqualTo(otherJob);
        return this;
    }


    private static class JobStateCondition extends Condition<Job> {

        public JobStateCondition(StateName stateName) {
            super(job -> job.hasState(stateName), "Job should have state %s", stateName);
        }
    }

}
