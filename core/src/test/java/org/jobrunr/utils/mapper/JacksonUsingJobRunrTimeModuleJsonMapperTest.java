package org.jobrunr.utils.mapper;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.jobrunr.JobRunrException;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jackson.modules.JobRunrTimeModule;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JacksonUsingJobRunrTimeModuleJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JacksonJsonMapper() {
            @Override
            protected Module getModule() {
                return new JobRunrTimeModule();
            }
        };
    }

    @Test
    void testSerializeAndDeserializeEnqueuedJobWithLocalDateTimeJobParameter() {
        assertThatThrownBy(super::testSerializeAndDeserializeEnqueuedJobWithLocalDateTimeJobParameter)
                .isInstanceOf(JobParameterJsonMapperException.class)
                .hasCauseInstanceOf(InvalidDefinitionException.class);
    }
}
