package org.jobrunr.quarkus.autoconfigure;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrProducerTest {

    JobRunrProducer jobRunrProducer;

    JobRunrConfiguration configuration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    JsonMapper jsonMapper;

    @Mock
    JobActivator jobActivator;

    @BeforeEach
    void setUp() {
        configuration = new JobRunrConfiguration();
        configuration.database = new JobRunrConfiguration.DatabaseConfiguration();
        configuration.jobs = new JobRunrConfiguration.JobsConfiguration();
        configuration.jobs.defaultNumberOfRetries = Optional.empty();
        configuration.jobs.retryBackOffTimeSeed = Optional.empty();
        configuration.jobScheduler = new JobRunrConfiguration.JobSchedulerConfiguration();
        configuration.jobScheduler.jobDetailsGenerator = Optional.empty();
        configuration.backgroundJobServer = new JobRunrConfiguration.BackgroundJobServerConfiguration();
        configuration.backgroundJobServer.pollIntervalInSeconds = Optional.empty();
        configuration.backgroundJobServer.workerCount = Optional.empty();
        configuration.backgroundJobServer.scheduledJobsRequestSize = Optional.empty();
        configuration.backgroundJobServer.orphanedJobsRequestSize = Optional.empty();
        configuration.backgroundJobServer.succeededsJobRequestSize = Optional.empty();
        configuration.backgroundJobServer.deleteSucceededJobsAfter = Optional.empty();
        configuration.backgroundJobServer.permanentlyDeleteDeletedJobsAfter = Optional.empty();
        configuration.dashboard = new JobRunrConfiguration.DashboardConfiguration();
        configuration.dashboard.port = Optional.empty();
        configuration.dashboard.username = Optional.empty();
        configuration.dashboard.password = Optional.empty();
        configuration.miscellaneous = new JobRunrConfiguration.MiscellaneousConfiguration();
        configuration.miscellaneous.allowAnonymousDataUsage = true;

        jobRunrProducer = new JobRunrProducer();
        setInternalState(jobRunrProducer, "configuration", configuration);
    }

    @Test
    void jobSchedulerIsNotSetupWhenConfigured() {
        configuration.jobScheduler.enabled = false;

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNull();
    }

    @Test
    void jobSchedulerIsSetupWhenConfigured() {
        configuration.jobScheduler.enabled = true;

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNotNull();
    }

    @Test
    void jobRequestSchedulerIsNotSetupWhenConfigured() {
        configuration.jobScheduler.enabled = false;

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNull();
    }

    @Test
    void jobRequestSchedulerIsSetupWhenConfigured() {
        configuration.jobScheduler.enabled = true;

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        configuration.backgroundJobServer.enabled = false;

        assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        configuration.backgroundJobServer.enabled = true;

        assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        configuration.backgroundJobServer.enabled = true;
        configuration.backgroundJobServer.pollIntervalInSeconds = Optional.of(5);
        configuration.backgroundJobServer.workerCount = Optional.of(4);
        configuration.backgroundJobServer.deleteSucceededJobsAfter = Optional.of(Duration.of(1, HOURS));
        configuration.backgroundJobServer.permanentlyDeleteDeletedJobsAfter = Optional.of(Duration.of(1, DAYS));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrProducer.backgroundJobServerConfiguration();
        assertThat(backgroundJobServerConfiguration).isNotNull();
        assertThat((int) getInternalState(backgroundJobServerConfiguration, "pollIntervalInSeconds")).isEqualTo(5);
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        configuration.backgroundJobServer.enabled = false;

        assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        configuration.backgroundJobServer.enabled = true;

        assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }

    @Test
    void dashboardWebServerConfigurationIsNotSetupWhenNotConfigured() {
        configuration.dashboard.enabled = false;

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNull();
    }

    @Test
    void dashboardWebServerConfigurationIsSetupWhenConfigured() {
        configuration.dashboard.enabled = true;

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNotNull();
    }

    @Test
    void dashboardWebServerIsNotSetupWhenNotConfigured() {
        configuration.dashboard.enabled = false;

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNull();
    }

    @Test
    void dashboardWebServerIsSetupWhenConfigured() {
        configuration.dashboard.enabled = true;

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNotNull();
    }
}
