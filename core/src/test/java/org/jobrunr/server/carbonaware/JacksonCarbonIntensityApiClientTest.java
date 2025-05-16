package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class JacksonCarbonIntensityApiClientTest extends AbstractCarbonIntensityApiClientTest {
    @Override
    protected JsonMapper getJsonMapper() {
        return new JacksonJsonMapper();
    }
}
