package org.jobrunr.dashboard;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;

public class JsonbJobRunrDashboardWebserverTest extends JobRunrDashboardWebServerTest {

    @Override
    public JsonMapper getJsonMapper() {
        return new JsonbJsonMapper();
    }

}
