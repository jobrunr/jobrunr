package org.jobrunr.tests.e2e;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.utils.reflection.ReflectionUtils.classExists;

public abstract class AbstractE2EJacksonSqlTest extends AbstractE2ESqlTest {

    @Test
    public void onlyJacksonIsOnClasspath() {
        assertThat(classExists("com.fasterxml.jackson.databind.ObjectMapper")).isTrue();
        assertThat(classExists("com.google.gson.Gson")).isFalse();
    }

}
