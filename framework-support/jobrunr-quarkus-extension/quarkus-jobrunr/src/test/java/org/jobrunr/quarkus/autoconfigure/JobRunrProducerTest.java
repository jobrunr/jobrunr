package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest
@TestProfile(JobRunrProducerTest.class)
class JobRunrProducerTest implements QuarkusTestProfile {

    @Inject
    JobRunrProducer jobRunrProducer;
    @Inject
    JobScheduler jobScheduler;
    @Inject
    JobRequestScheduler jobRequestScheduler;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "true")
    void jobSchedulerIsSetupWhenConfigured() {
        assertThat(CDI.current().select(JobScheduler.class).isResolvable()).isTrue();
        assertThat(CDI.current().select(JobRequestScheduler.class).isResolvable()).isTrue();
    }
}
