package org.jobrunr.quarkus.autoconfigure;

import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusComponentTest(JobRunrProducer.class)
class JobRunrProducerTest {

    @Inject
    Instance<JobScheduler> jobSchedulerInstance;
    @Inject
    Instance<JobRequestScheduler> jobRequestSchedulerInstance;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "true")
    void jobSchedulerIsSetupWhenConfigured() {
        assertThat(jobSchedulerInstance.isResolvable()).isTrue();
        assertThat(jobSchedulerInstance.get()).isInstanceOf(JobScheduler.class);

        assertThat(jobRequestSchedulerInstance.isResolvable()).isTrue();
        assertThat(jobRequestSchedulerInstance.get()).isInstanceOf(JobRequestScheduler.class);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.job-scheduler.enabled", value = "false")
    void jobSchedulerIsNotSetUpWhenDisabled() {
        assertThat(jobSchedulerInstance.isResolvable()).isTrue();
        assertThat(jobSchedulerInstance.get()).isNull();

        assertThat(jobRequestSchedulerInstance.isResolvable()).isTrue();
        assertThat(jobRequestSchedulerInstance.get()).isNull();
    }
}
