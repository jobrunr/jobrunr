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

    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;

    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    JsonMapper jsonMapper;

    @Mock
    JobActivator jobActivator;

    @BeforeEach
    void setUp() {
        jobRunrBuildTimeConfiguration = new JobRunrBuildTimeConfiguration();
        jobRunrBuildTimeConfiguration.jobScheduler = new JobRunrBuildTimeConfiguration.JobSchedulerConfiguration();
        jobRunrBuildTimeConfiguration.backgroundJobServer = new JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration();
        jobRunrBuildTimeConfiguration.dashboard = new JobRunrBuildTimeConfiguration.DashboardConfiguration();

        jobRunrRuntimeConfiguration = new JobRunrRuntimeConfiguration();
        jobRunrRuntimeConfiguration.database = new JobRunrRuntimeConfiguration.DatabaseConfiguration();
        jobRunrRuntimeConfiguration.jobs = new JobRunrRuntimeConfiguration.JobsConfiguration();
        jobRunrRuntimeConfiguration.jobs.defaultNumberOfRetries = Optional.empty();
        jobRunrRuntimeConfiguration.jobs.retryBackOffTimeSeed = Optional.empty();
        jobRunrRuntimeConfiguration.jobScheduler = new JobRunrRuntimeConfiguration.JobSchedulerConfiguration();
        jobRunrRuntimeConfiguration.jobScheduler.jobDetailsGenerator = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer = new JobRunrRuntimeConfiguration.BackgroundJobServerConfiguration();
        jobRunrRuntimeConfiguration.backgroundJobServer.name = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.pollIntervalInSeconds = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.workerCount = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.scheduledJobsRequestSize = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.orphanedJobsRequestSize = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.succeededsJobRequestSize = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.deleteSucceededJobsAfter = Optional.empty();
        jobRunrRuntimeConfiguration.backgroundJobServer.permanentlyDeleteDeletedJobsAfter = Optional.empty();
        jobRunrRuntimeConfiguration.dashboard = new JobRunrRuntimeConfiguration.DashboardConfiguration();
        jobRunrRuntimeConfiguration.dashboard.port = Optional.empty();
        jobRunrRuntimeConfiguration.dashboard.username = Optional.empty();
        jobRunrRuntimeConfiguration.dashboard.password = Optional.empty();
        jobRunrRuntimeConfiguration.miscellaneous = new JobRunrRuntimeConfiguration.MiscellaneousConfiguration();
        jobRunrRuntimeConfiguration.miscellaneous.allowAnonymousDataUsage = true;

        jobRunrProducer = new JobRunrProducer();
        setInternalState(jobRunrProducer, "jobRunrBuildTimeConfiguration", jobRunrBuildTimeConfiguration);
        setInternalState(jobRunrProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void jobSchedulerIsNotSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.jobScheduler.enabled = false;

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNull();
    }

    @Test
    void jobSchedulerIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.jobScheduler.enabled = true;

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNotNull();
    }

    @Test
    void jobRequestSchedulerIsNotSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.jobScheduler.enabled = false;

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNull();
    }

    @Test
    void jobRequestSchedulerIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.jobScheduler.enabled = true;

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        jobRunrBuildTimeConfiguration.backgroundJobServer.enabled = false;

        assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.backgroundJobServer.enabled = true;

        assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        jobRunrBuildTimeConfiguration.backgroundJobServer.enabled = true;

        jobRunrRuntimeConfiguration.backgroundJobServer.name = Optional.of("test");
        jobRunrRuntimeConfiguration.backgroundJobServer.pollIntervalInSeconds = Optional.of(5);
        jobRunrRuntimeConfiguration.backgroundJobServer.workerCount = Optional.of(4);
        jobRunrRuntimeConfiguration.backgroundJobServer.deleteSucceededJobsAfter = Optional.of(Duration.of(1, HOURS));
        jobRunrRuntimeConfiguration.backgroundJobServer.permanentlyDeleteDeletedJobsAfter = Optional.of(Duration.of(1, DAYS));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrProducer.backgroundJobServerConfiguration();
        assertThat(backgroundJobServerConfiguration).isNotNull();
        assertThat((String) getInternalState(backgroundJobServerConfiguration, "name")).isEqualTo("test");
        assertThat((int) getInternalState(backgroundJobServerConfiguration, "pollIntervalInSeconds")).isEqualTo(5);
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        jobRunrBuildTimeConfiguration.backgroundJobServer.enabled = false;

        assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.backgroundJobServer.enabled = true;

        assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }

    @Test
    void dashboardWebServerConfigurationIsNotSetupWhenNotConfigured() {
        jobRunrBuildTimeConfiguration.dashboard.enabled = false;

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNull();
    }

    @Test
    void dashboardWebServerConfigurationIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.dashboard.enabled = true;

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNotNull();
    }

    @Test
    void dashboardWebServerIsNotSetupWhenNotConfigured() {
        jobRunrBuildTimeConfiguration.dashboard.enabled = false;

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNull();
    }

    @Test
    void dashboardWebServerIsSetupWhenConfigured() {
        jobRunrBuildTimeConfiguration.dashboard.enabled = true;

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNotNull();
    }
}
