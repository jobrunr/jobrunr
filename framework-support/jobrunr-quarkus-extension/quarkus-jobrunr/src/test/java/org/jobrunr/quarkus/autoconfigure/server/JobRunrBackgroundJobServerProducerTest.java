package org.jobrunr.quarkus.autoconfigure.server;


import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.TestConfigProperty;
import jakarta.inject.Inject;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.jobrunr.JobRunrAssertions.assertThat;

@QuarkusComponentTest
public class JobRunrBackgroundJobServerProducerTest {

    // Injection needed to create all other beans otherwise the extension doesn't pick them up.
    @Inject
    JobRunrBackgroundJobServerProducer jobRunrBackgroundJobServerProducer;

    @Inject
    BackgroundJobServer backgroundJobServer;
    @Inject
    BackgroundJobServerConfiguration backgroundJobServerConfiguration;

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    void carbonAwareManagerAutoConfigurationIsDisabledByDefault() {
        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingConfiguration();
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
        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingConfiguration();
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
        assertThat(backgroundJobServer).hasName("test");
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.worker-count", value = "4")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.thread-type", value = "PlatformThreads")
    void backgroundJobServerAutoConfigurationTakesIntoThreadTypeAndWorkerCount() {
        assertThat(backgroundJobServerConfiguration)
                .hasWorkerCount(4);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.poll-interval-in-seconds", value = "5")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.server-timeout-poll-interval-multiplicand", value = "10")
    void backgroundJobServerAutoConfigurationTakesAllBackgroundServerPollIntervals() {
        assertThat(backgroundJobServerConfiguration)
                .hasPollIntervalInSeconds(5)
                .hasServerTimeoutPollIntervalMultiplicand(10);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.jobs.default-number-of-retries", value = "3")
    void backgroundJobServerAutoConfigurationTakesIntoAccountDefaultNumberOfRetries() {
        assertThat(backgroundJobServer)
                .hasRetryFilter(3);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.scheduled-jobs-request-size", value = "1")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.orphaned-jobs-request-size", value = "2")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.succeeded-jobs-request-size", value = "3")
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllJobsRequestSizes() {
        assertThat(backgroundJobServerConfiguration)
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3);
    }

    @Test
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.enabled", value = "true")
    @TestConfigProperty(key = "quarkus.jobrunr.background-job-server.interrupt-jobs-await-duration-on-stop", value = "20")
    void backgroundJobServerAutoConfigurationTakesIntoAccountInterruptJobsAwaitDurationOnStopBackgroundJobServer() {
        assertThat(backgroundJobServerConfiguration)
                .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20));
    }


}
