package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;

public class GsonCarbonIntensityApiClientTest extends AbstractCarbonIntensityApiClientTest {
    @Override
    protected JsonMapper getJsonMapper() {
        return new GsonJsonMapper();
    }
}
