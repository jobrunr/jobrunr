package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMapperFactoryTest {

    @Test
    void testCreateJsonMapper() {
        JsonMapper jsonMapper = JsonMapperFactory.createJsonMapper();
        assertThat(jsonMapper)
                .isNotNull()
                .isInstanceOf(Jackson3JsonMapper.class);
    }
}