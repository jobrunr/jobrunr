package org.jobrunr.jobs.mappers;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class GsonJobMapperTest extends JobMapperTest {

    @Override
    protected JsonMapper getJsonMapper() {
        return new GsonJsonMapper();
    }

    @Disabled("GSON does not throw an exception when serializing circular references ")
    @Test
    void onIllegalJobParameterCorrectExceptionIsThrown() {
        super.onIllegalJobParameterCorrectExceptionIsThrown();
    }
}
