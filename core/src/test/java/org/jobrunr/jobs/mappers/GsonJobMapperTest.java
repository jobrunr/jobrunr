package org.jobrunr.jobs.mappers;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;

public class GsonJobMapperTest extends JobMapperTest {

    @Override
    protected JsonMapper getJsonMapper() {
        return new GsonJsonMapper();
    }
}
