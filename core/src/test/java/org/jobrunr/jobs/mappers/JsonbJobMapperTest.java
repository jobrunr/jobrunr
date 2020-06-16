package org.jobrunr.jobs.mappers;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

public class JsonbJobMapperTest extends JobMapperTest {
    @Override
    protected JsonMapper getJsonMapper() {
        return new JsonbJsonMapper();
    }
}
