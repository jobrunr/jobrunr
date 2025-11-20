package org.jobrunr.dashboard.sse;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson3.Jackson3JsonMapper;

public class Jackson3JobStatsSseExchangeTest extends AbstractJobStatsSseExchangeTest {

    @Override
    protected JsonMapper jsonMapper() {
        return new Jackson3JsonMapper();
    }
}