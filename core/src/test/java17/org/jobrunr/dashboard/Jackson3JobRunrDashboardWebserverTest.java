package org.jobrunr.dashboard;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.Jackson3JsonMapper;

public class Jackson3JobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new Jackson3JsonMapper();
    }

}
