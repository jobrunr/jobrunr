package org.jobrunr.utils.mapper.jackson;

import org.jobrunr.utils.mapper.AbstractJsonMapperTest;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson3JsonMapperTest extends AbstractJsonMapperTest {

    @Override
    public JsonMapper newJsonMapper() {
        return new Jackson3JsonMapper();
    }

    @Test
    void expectToFailToMakeSureJava17TestsRunInCI() {
        assertThat(true).isFalse();
    }
}