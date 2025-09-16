package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Must be in another test as Quarkus initializes the test context once per class
@QuarkusComponentTest
public class JobRunrProducerJobSchedulerDisabledTest {

    @Inject
    JobRunrProducer jobRunrProducer;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "false")
    void jobSchedulerIsNotSetUpWhenDisabled() {
        assertThat(CDI.current().select(JobScheduler.class).isResolvable()).isFalse();
        assertThat(CDI.current().select(JobRequestScheduler.class).isResolvable()).isFalse();
    }
}
