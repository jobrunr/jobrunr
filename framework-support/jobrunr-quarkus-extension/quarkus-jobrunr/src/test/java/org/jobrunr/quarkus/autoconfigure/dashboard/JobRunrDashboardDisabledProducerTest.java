package org.jobrunr.quarkus.autoconfigure.dashboard;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@QuarkusComponentTest
@TestProfile(JobRunrDashboardDisabledProducerTest.class)
public class JobRunrDashboardDisabledProducerTest implements QuarkusTestProfile {

    // Injection needed to create all other beans otherwise the extension doesn't pick them up.
    @Inject
    JobRunrDashboardProducer jobRunrDashboardProducer;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.dashboard.enabled", value = "false")
    void dashboardWebServerBeanIsNotThereWhenDisabled() {
        var bean = CDI.current().select(JobRunrDashboardWebServer.class);
        assertThat(bean.isResolvable()).isFalse();
    }

}
