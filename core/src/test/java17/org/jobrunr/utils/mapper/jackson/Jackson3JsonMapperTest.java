package org.jobrunr.utils.mapper.jackson;

import org.jobrunr.utils.mapper.AbstractJsonMapperTest;
import org.jobrunr.utils.mapper.JsonMapper;

class Jackson3JsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new Jackson3JsonMapper();
    }
}