package org.jobrunr.quarkus.autoconfigure;

import org.assertj.core.api.Assertions;
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
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrProducerTest {

    JobRunrProducer jobRunrProducer;

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.JobSchedulerConfiguration jobSchedulerBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.BackgroundJobServerConfiguration backgroundJobServerBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.DashboardConfiguration dashboardBuildTimeConfiguration;


    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.DatabaseConfiguration databaseRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobsConfiguration jobsRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobSchedulerConfiguration jobSchedulerRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.BackgroundJobServerConfiguration backgroundJobServerRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.DashboardConfiguration dashboardRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.MiscellaneousConfiguration miscellaneousRunTimeConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    JsonMapper jsonMapper;

    @Mock
    JobActivator jobActivator;

    @BeforeEach
    void setUp() {
        lenient().when(jobRunrBuildTimeConfiguration.jobScheduler()).thenReturn(jobSchedulerBuildTimeConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerBuildTimeConfiguration);
        lenient().when(jobRunrBuildTimeConfiguration.dashboard()).thenReturn(dashboardBuildTimeConfiguration);

        lenient().when(jobRunrRuntimeConfiguration.database()).thenReturn(databaseRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobs()).thenReturn(jobsRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.miscellaneous()).thenReturn(miscellaneousRunTimeConfiguration);

        jobRunrProducer = new JobRunrProducer();
        setInternalState(jobRunrProducer, "jobRunrBuildTimeConfiguration", jobRunrBuildTimeConfiguration);
        setInternalState(jobRunrProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void jobSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNull();
    }

    @Test
    void jobSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNotNull();
    }

    @Test
    void jobRequestSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNull();
    }

    @Test
    void jobRequestSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration()).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        when(backgroundJobServerRunTimeConfiguration.name()).thenReturn(Optional.of("test"));
        when(backgroundJobServerRunTimeConfiguration.pollIntervalInSeconds()).thenReturn(Optional.of(5));
        when(backgroundJobServerRunTimeConfiguration.workerCount()).thenReturn(Optional.of(4));
        when(backgroundJobServerRunTimeConfiguration.scheduledJobsRequestSize()).thenReturn(Optional.of(1));
        when(backgroundJobServerRunTimeConfiguration.orphanedJobsRequestSize()).thenReturn(Optional.of(2));
        when(backgroundJobServerRunTimeConfiguration.succeededJobRequestSize()).thenReturn(Optional.of(3));
        when(backgroundJobServerRunTimeConfiguration.deleteSucceededJobsAfter()).thenReturn(Optional.of(Duration.of(1, HOURS)));
        when(backgroundJobServerRunTimeConfiguration.permanentlyDeleteDeletedJobsAfter()).thenReturn(Optional.of(Duration.of(1, DAYS)));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrProducer.backgroundJobServerConfiguration();
        assertThat(backgroundJobServerConfiguration)
                .isNotNull()
                .hasName("test")
                .hasPollIntervalInSeconds(5)
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3);
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }

    @Test
    void dashboardWebServerConfigurationIsNotSetupWhenNotConfigured() {
        when(dashboardBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNull();
    }

    @Test
    void dashboardWebServerConfigurationIsSetupWhenConfigured() {
        when(dashboardBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNotNull();
    }

    @Test
    void dashboardWebServerIsNotSetupWhenNotConfigured() {
        when(dashboardBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNull();
    }

    @Test
    void dashboardWebServerIsSetupWhenConfigured() {
        when(dashboardBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNotNull();
    }
}
