package org.jobrunr.utils.mapper;

import org.jobrunr.utils.mapper.gson.GsonJsonMapper;

public class GsonJsonMapperTest extends JsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new GsonJsonMapper();
    }

}
