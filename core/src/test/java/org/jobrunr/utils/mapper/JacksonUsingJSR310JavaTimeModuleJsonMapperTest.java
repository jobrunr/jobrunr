package org.jobrunr.utils.mapper;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jobrunr.jobs.Job;
import org.jobrunr.utils.annotations.Because;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Objects;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class JacksonUsingJSR310JavaTimeModuleJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JacksonJsonMapper();
    }

    @Test
    void testTypeIdJavaTimeModule() {
        assertThat(new JavaTimeModule().getTypeId()).isEqualTo("jackson-datatype-jsr310");
    }

    @Override
    @Test
    @Disabled
    void testSerializeAndDeserializeEnqueuedJobWithOffsetDateTimeJobParameter() {
        // Jackson Offset date time differs?
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

    // we cannot use records as the test fixtures are compiled with Java 11 and they are used by other people
    // for JobRunr 7, bump the testfixtures to Java 17 and test with an actual record
    // Note for future self: I'm really sorry about this :-s
    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/779")
    @Deprecated
    void testCreatorHasDefaultVisibilityInJacksonObjectMapper() {
        Object objectMapper = Whitebox.getInternalState(jsonMapper, "objectMapper");
        Object configOverrides = Whitebox.getInternalState(objectMapper, "_configOverrides");
        Object visibilityChecker = Whitebox.getInternalState(configOverrides, "_visibilityChecker");
        assertThat(visibilityChecker.toString()).contains("creator=ANY");
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
            if (!(o instanceof SomeParameter)) return false;
            SomeParameter that = (SomeParameter) o;
            return value == that.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}