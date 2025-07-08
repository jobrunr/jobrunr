package org.jobrunr.dashboard.sse;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class JacksonJobStatsSseExchangeTest extends AbstractJobStatsSseExchangeTest {

    @Override
    protected JsonMapper jsonMapper() {
        return new JacksonJsonMapper();
    }
}