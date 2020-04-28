package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

class JacksonUsingJSR310JavaTimeModuleJsonMapperTest extends JsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JacksonJsonMapper();
    }
}