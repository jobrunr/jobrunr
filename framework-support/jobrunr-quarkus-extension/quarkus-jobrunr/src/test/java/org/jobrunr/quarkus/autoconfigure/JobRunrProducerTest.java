package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest
class JobRunrProducerTest {

    @Inject
    Instance<JobRunrProducer> jobRunrProducer;
    @Inject
    Instance<JobScheduler> jobScheduler;
    @Inject
    Instance<JobRequestScheduler> jobRequestScheduler;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "true")
    void jobSchedulerIsSetupWhenConfigured() {
        assertThat(CDI.current().select(JobScheduler.class).isResolvable()).isTrue();
        assertThat(CDI.current().select(JobScheduler.class).get()).isInstanceOf(JobScheduler.class);

        assertThat(CDI.current().select(JobRequestScheduler.class).isResolvable()).isTrue();
        assertThat(CDI.current().select(JobRequestScheduler.class).get()).isInstanceOf(JobRequestScheduler.class);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "false")
    void jobSchedulerIsNotSetUpWhenDisabled() {
        assertThat(CDI.current().select(JobScheduler.class).isResolvable()).isTrue();
        assertThat(CDI.current().select(JobScheduler.class).get()).isNull();

        assertThat(CDI.current().select(JobRequestScheduler.class).isResolvable()).isTrue();
        assertThat(CDI.current().select(JobRequestScheduler.class).get()).isNull();
    }
}
