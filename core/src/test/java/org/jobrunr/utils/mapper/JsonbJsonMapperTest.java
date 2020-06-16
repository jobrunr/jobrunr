package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

public class JsonbJsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new JsonbJsonMapper();
    }

}