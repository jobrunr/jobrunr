package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest(value = JobRunrDashboardProducer.class) // needed to create all other beans otherwise the extension doesn't pick them up.

class JobRunrDashboardProducerTest {

    @Inject
    Instance<JobRunrDashboardWebServer> jobRunrDashboardWebServer;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "true")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        assertThat(jobRunrDashboardWebServer.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServer.get())
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "false")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        assertThat(jobRunrDashboardWebServer.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServer.get())
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "false")
    void dashboardWebServerBeanIsNotThereWhenDisabled() {
        assertThat(jobRunrDashboardWebServer.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServer.get()).isNull();
    }
}