package org.jobrunr.micronaut.autoconfigure.health;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
@Property(name = "jobrunr.database.type", value = "mem")
@TestMethodOrder(MethodOrderer.MethodName.class)
class JobRunrHealthIndicatorRegistrationTest {

    @Inject
    ApplicationContext context;

    @Test
    void aFirstTestThatReloadsTheContextToMakeFlakyTestWork() {
        assertThat(context).hasSingleBean(StorageProvider.class);
    }

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
