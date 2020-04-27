package org.jobrunr.jobs.mappers;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class JacksonJobMapperTest extends JobMapperTest {

    @Override
    protected JsonMapper getJsonMapper() {
        return new JacksonJsonMapper();
    }
}
