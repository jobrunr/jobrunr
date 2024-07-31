package org.jobrunr.quarkus.autoconfigure;

import org.assertj.core.api.Assertions;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.configuration.BackgroundJobServerWorkerPolicy;
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
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.dashboard.JobRunrDashboardWebServerConfiguration.usingStandardDashboardConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
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
    JobRunrRuntimeConfiguration.CarbonAwareConfiguration carbonAwareRunTimeConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    CarbonAwareJobManager carbonAwareJobManager;

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
        lenient().when(jobRunrRuntimeConfiguration.jobs().carbonAwareConfiguration()).thenReturn(carbonAwareRunTimeConfiguration);

        jobRunrProducer = new JobRunrProducer();
        setInternalState(jobRunrProducer, "jobRunrBuildTimeConfiguration", jobRunrBuildTimeConfiguration);
        setInternalState(jobRunrProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void jobSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobScheduler(storageProvider, carbonAwareJobManager)).isNull();
    }

    @Test
    void jobSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobScheduler(storageProvider, carbonAwareJobManager)).isNotNull();
    }

    @Test
    void jobRequestSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider, carbonAwareJobManager)).isNull();
    }

    @Test
    void jobRequestSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerBuildTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider, carbonAwareJobManager)).isNotNull();
    }

    @Test
    void carbonAwareJobManagerIsSetupWhenConfigured() {
        when(carbonAwareRunTimeConfiguration.areaCode()).thenReturn(Optional.of("DE"));
        when(carbonAwareRunTimeConfiguration.apiClientConnectTimeoutMs()).thenReturn(Optional.of(500));
        when(carbonAwareRunTimeConfiguration.apiClientReadTimeoutMs()).thenReturn(Optional.of(1000));

        CarbonAwareJobManager carbonAwareJobManager = jobRunrProducer.carbonAwareJobManager(jsonMapper);
        CarbonAwareConfigurationReader carbonAwareConfiguration = getInternalState(carbonAwareJobManager, "carbonAwareConfiguration");

        assertThat(carbonAwareConfiguration).
                hasAreaCode("DE")
                .hasApiClientConnectTimeout(Duration.ofMillis(500))
                .hasApiClientReadTimeout(Duration.ofMillis(1000));
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        when(backgroundJobServerRunTimeConfiguration.name()).thenReturn(Optional.of("test"));
        when(backgroundJobServerRunTimeConfiguration.pollIntervalInSeconds()).thenReturn(Optional.of(5));
        when(backgroundJobServerRunTimeConfiguration.scheduledJobsRequestSize()).thenReturn(Optional.of(1));
        when(backgroundJobServerRunTimeConfiguration.orphanedJobsRequestSize()).thenReturn(Optional.of(2));
        when(backgroundJobServerRunTimeConfiguration.succeededJobRequestSize()).thenReturn(Optional.of(3));
        when(backgroundJobServerRunTimeConfiguration.deleteSucceededJobsAfter()).thenReturn(Optional.of(Duration.of(1, HOURS)));
        when(backgroundJobServerRunTimeConfiguration.permanentlyDeleteDeletedJobsAfter()).thenReturn(Optional.of(Duration.of(1, DAYS)));
        when(backgroundJobServerRunTimeConfiguration.interruptJobsAwaitDurationOnStop()).thenReturn(Optional.of(Duration.of(20, SECONDS)));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class));
        assertThat(backgroundJobServerConfiguration)
                .isNotNull()
                .hasName("test")
                .hasPollIntervalInSeconds(5)
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3)
                .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration(), null)).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        when(backgroundJobServerBuildTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration(), null)).isNotNull();
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
