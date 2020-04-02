package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

class JacksonJsonMapperTest extends JsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JacksonJsonMapper();
    }
}