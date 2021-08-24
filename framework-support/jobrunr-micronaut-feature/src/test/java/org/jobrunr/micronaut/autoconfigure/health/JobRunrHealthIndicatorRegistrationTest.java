package org.jobrunr.micronaut.autoconfigure.health;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
class JobRunrHealthIndicatorRegistrationTest {

    @Inject
    ApplicationContext context;

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void jobRunrHealthIndicatorEnabledAutoConfiguration() {
        assertThat(context).hasSingleBean(JobRunrHealthIndicator.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "false")
    void jobRunrHealthIndicatorEnabledButBackgroundJobServerDisabledAutoConfiguration() {
        assertThat(context).doesNotHaveBean(JobRunrHealthIndicator.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    @Property(name = "jobrunr.health.enabled", value = "false")
    void jobRunrHealthIndicatorDisabledAutoConfiguration() {
        assertThat(context).doesNotHaveBean(JobRunrHealthIndicator.class);
    }
}
