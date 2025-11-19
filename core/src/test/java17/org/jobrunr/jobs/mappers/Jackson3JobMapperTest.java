package org.jobrunr.jobs.mappers;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;

public class Jackson3JobMapperTest extends JobMapperTest {

    @Override
    protected JsonMapper getJsonMapper() {
        return new Jackson3JsonMapper();
    }
}
