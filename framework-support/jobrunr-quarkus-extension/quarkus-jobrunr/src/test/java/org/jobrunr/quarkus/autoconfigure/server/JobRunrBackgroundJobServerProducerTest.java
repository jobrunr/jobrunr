package org.jobrunr.quarkus.autoconfigure.server;


import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.jobrunr.JobRunrAssertions.assertThat;

@QuarkusComponentTest(JobRunrBackgroundJobServerProducer.class) // needed to create all other beans otherwise the extension doesn't pick them up.
public class JobRunrBackgroundJobServerProducerTest {

    @Inject
    Instance<BackgroundJobServer> backgroundJobServerInstance;
    @Inject
    Instance<BackgroundJobServerConfiguration> backgroundJobServerConfigurationInstance;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    void carbonAwareManagerAutoConfigurationIsDisabledByDefault() {
        assertThat(backgroundJobServerInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerInstance.get()).isInstanceOf(BackgroundJobServer.class);

        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServerInstance.get().getConfiguration().getCarbonAwareJobProcessingConfiguration();
        assertThat(carbonAwareJobProcessingConfiguration).hasEnabled(false);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.carbon-aware-job-processing.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.carbon-aware-job-processing.area-code", value = "FR")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.carbon-aware-job-processing.api-client-connect-timeout", value = "500ms")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.carbon-aware-job-processing.api-client-read-timeout", value = "300ms")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.carbon-aware-job-processing.poll-interval-in-minutes", value = "15")
    void carbonAwareManagerAutoConfiguration() {
        assertThat(backgroundJobServerInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerInstance.get()).isInstanceOf(BackgroundJobServer.class);

        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServerInstance.get().getConfiguration().getCarbonAwareJobProcessingConfiguration();
        assertThat(carbonAwareJobProcessingConfiguration)
                .hasEnabled(true)
                .hasApiClientConnectTimeout(Duration.ofMillis(500))
                .hasApiClientReadTimeout(Duration.ofMillis(300))
                .hasPollIntervalInMinutes(15)
                .hasAreaCode("FR");
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.name", value = "test")
    void backgroundJobServerAutoConfigurationTakesIntoAccountName() {
        assertThat(backgroundJobServerInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerInstance.get()).isInstanceOf(BackgroundJobServer.class);

        assertThat(backgroundJobServerInstance.get()).hasName("test");
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.worker-count", value = "4")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.thread-type", value = "PlatformThreads")
    void backgroundJobServerAutoConfigurationTakesIntoThreadTypeAndWorkerCount() {
        assertThat(backgroundJobServerConfigurationInstance.isResolvable()).isTrue();
        Assertions.assertThat(backgroundJobServerConfigurationInstance.get()).isInstanceOf(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfigurationInstance.get())
                .hasWorkerCount(4);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.poll-interval-in-seconds", value = "5")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.server-timeout-poll-interval-multiplicand", value = "10")
    void backgroundJobServerAutoConfigurationTakesAllBackgroundServerPollIntervals() {
        assertThat(backgroundJobServerConfigurationInstance.isResolvable()).isTrue();
        Assertions.assertThat(backgroundJobServerConfigurationInstance.get()).isInstanceOf(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfigurationInstance.get())
                .hasPollIntervalInSeconds(5)
                .hasServerTimeoutPollIntervalMultiplicand(10);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.jobs.default-number-of-retries", value = "3")
    void backgroundJobServerAutoConfigurationTakesIntoAccountDefaultNumberOfRetries() {
        assertThat(backgroundJobServerInstance.isResolvable()).isTrue();
        assertThat(backgroundJobServerInstance.get()).isInstanceOf(BackgroundJobServer.class);

        assertThat(backgroundJobServerInstance.get())
                .hasRetryFilter(3);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.scheduled-jobs-request-size", value = "1")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.orphaned-jobs-request-size", value = "2")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.succeeded-jobs-request-size", value = "3")
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllJobsRequestSizes() {
        assertThat(backgroundJobServerConfigurationInstance.isResolvable()).isTrue();
        Assertions.assertThat(backgroundJobServerConfigurationInstance.get()).isInstanceOf(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfigurationInstance.get())
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.interrupt-jobs-await-duration-on-stop", value = "20")
    void backgroundJobServerAutoConfigurationTakesIntoAccountInterruptJobsAwaitDurationOnStopBackgroundJobServer() {
        assertThat(backgroundJobServerConfigurationInstance.isResolvable()).isTrue();
        Assertions.assertThat(backgroundJobServerConfigurationInstance.get()).isInstanceOf(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfigurationInstance.get())
                .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20));
    }
}
