package org.jobrunr.utils.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.jobrunr.jobs.Job;
import org.jobrunr.utils.annotations.Because;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

public class JacksonUsingJobRunrTimeModuleJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JacksonJsonMapper(false);
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithLocalDateTimeJobParameter() {
        assertThatThrownBy(super::testSerializeAndDeserializeEnqueuedJobWithLocalDateTimeJobParameter)
                .isInstanceOf(JobParameterJsonMapperException.class)
                .hasCauseInstanceOf(InvalidDefinitionException.class);
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/451")
    void testCanDeserializeWithJsonCreator() {
        SomeParameter someParameter = new SomeParameter(3);
        Job job = anEnqueuedJob()
                .withJobDetails(() -> doWorkWithParameter(someParameter))
                .build();

        String jobAsString = jsonMapper.serialize(job);

        Job deserializedJob = jsonMapper.deserialize(jobAsString, Job.class);
        assertThat(deserializedJob.getJobDetails())
                .hasArgs(someParameter);
    }

    public void doWorkWithParameter(SomeParameter parameter) {
        System.out.println(parameter);
    }

    public static class SomeParameter {

        private final int value;

        @JsonCreator
        public SomeParameter(@JsonProperty("value") int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "Some Parameter " + value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SomeParameter that = (SomeParameter) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
