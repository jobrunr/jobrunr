package org.jobrunr.jobs;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.data.TemporalOffset;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.stream.Collectors;

public class JobAssert extends AbstractAssert<JobAssert, Job> {

    private JobAssert(Job job) {
        super(job, JobAssert.class);
    }

    public static JobAssert assertThat(Job job) {
        return new JobAssert(job);
    }

    public JobAssert hasJobName(String name) {
        Assertions.assertThat(actual.getJobName()).isEqualTo(name);
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

    public JobAssert doesNotHaveState(StateName stateName) {
        Assertions.assertThat(actual.getState()).isNotEqualTo(stateName);
        return this;
    }

    public JobAssert hasStates(StateName... state) {
        List<StateName> jobStates = actual.getJobStates().stream().map(JobState::getName).collect(Collectors.toList());
        Assertions.assertThat(jobStates).containsExactly(state);
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

    public JobAssert hasVersion(int version) {
        Assertions.assertThat(actual.getVersion()).isEqualTo(version);
        return this;
    }

    public JobAssert isEqualTo(Job otherJob) {
        Assertions.assertThat(actual)
                .usingRecursiveComparison()
                .usingOverriddenEquals()
                .ignoringFields("locker")
                .isEqualTo(otherJob);
        return this;
    }

}
