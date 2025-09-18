package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest(JobRunrDashboardProducer.class) // needed to create all other beans otherwise the extension doesn't pick them up.

class JobRunrDashboardProducerTest {

    @Inject
    Instance<JobRunrDashboardWebServer> jobRunrDashboardWebServerInstance;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "true")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        assertThat(jobRunrDashboardWebServerInstance.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServerInstance.get())
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "false")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        assertThat(jobRunrDashboardWebServerInstance.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServerInstance.get())
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "false")
    void dashboardWebServerBeanIsNotThereWhenDisabled() {
        assertThat(jobRunrDashboardWebServerInstance.isResolvable()).isTrue();
        assertThat(jobRunrDashboardWebServerInstance.get()).isNull();
    }
}