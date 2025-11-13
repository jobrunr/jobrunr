package org.jobrunr.server.carbonaware;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.Jackson3JsonMapper;

public class Jackson3CarbonIntensityApiClientTest extends AbstractCarbonIntensityApiClientTest {
    @Override
    protected JsonMapper getJsonMapper() {
        return new Jackson3JsonMapper();
    }
}
