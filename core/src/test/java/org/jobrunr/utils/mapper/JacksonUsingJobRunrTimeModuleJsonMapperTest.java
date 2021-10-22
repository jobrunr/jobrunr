package org.jobrunr.utils.mapper;

import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
}
