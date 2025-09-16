package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest
class JobRunrDashboardProducerTest {

    // Injection needed to create all other beans otherwise the extension doesn't pick them up.
    @Inject
    JobRunrDashboardProducer jobRunrDashboardProducer;

    @Inject
    JobRunrDashboardWebServer jobRunrDashboardWebServer;
    
    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "true")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        assertThat(jobRunrDashboardWebServer)
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.miscellaneous.allow-anonymous-data-usage", value = "false")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        assertThat(jobRunrDashboardWebServer)
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
    }
}