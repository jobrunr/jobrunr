package org.jobrunr.quarkus.autoconfigure;

import org.assertj.core.api.Assertions;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.configuration.BackgroundJobServerThreadType;
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
        lenient().when(jobRunrRuntimeConfiguration.database()).thenReturn(databaseRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobs()).thenReturn(jobsRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.miscellaneous()).thenReturn(miscellaneousRunTimeConfiguration);

        jobRunrProducer = new JobRunrProducer();
        setInternalState(jobRunrProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void jobSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNull();
    }

    @Test
    void jobSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobScheduler(storageProvider)).isNotNull();
    }

    @Test
    void jobRequestSchedulerIsNotSetupWhenConfigured() {
        when(jobSchedulerRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNull();
    }

    @Test
    void jobRequestSchedulerIsSetupWhenConfigured() {
        when(jobSchedulerRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.jobRequestScheduler(storageProvider)).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        when(backgroundJobServerRunTimeConfiguration.name()).thenReturn(Optional.of("test"));
        when(backgroundJobServerRunTimeConfiguration.workerCount()).thenReturn(Optional.of(25));
        when(backgroundJobServerRunTimeConfiguration.threadType()).thenReturn(Optional.of(BackgroundJobServerThreadType.PlatformThreads));
        when(backgroundJobServerRunTimeConfiguration.pollIntervalInSeconds()).thenReturn(Optional.of(5));
        when(backgroundJobServerRunTimeConfiguration.serverTimeoutPollIntervalMultiplicand()).thenReturn(Optional.of(10));
        when(backgroundJobServerRunTimeConfiguration.scheduledJobsRequestSize()).thenReturn(Optional.of(1));
        when(backgroundJobServerRunTimeConfiguration.orphanedJobsRequestSize()).thenReturn(Optional.of(2));
        when(backgroundJobServerRunTimeConfiguration.succeededJobRequestSize()).thenReturn(Optional.of(3));
        when(backgroundJobServerRunTimeConfiguration.deleteSucceededJobsAfter()).thenReturn(Optional.of(Duration.of(1, HOURS)));
        when(backgroundJobServerRunTimeConfiguration.permanentlyDeleteDeletedJobsAfter()).thenReturn(Optional.of(Duration.of(1, DAYS)));
        when(backgroundJobServerRunTimeConfiguration.interruptJobsAwaitDurationOnStop()).thenReturn(Optional.of(Duration.of(20, SECONDS)));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrProducer.backgroundJobServerConfiguration(jobRunrProducer.backgroundJobServerWorkerPolicy());
        assertThat(backgroundJobServerConfiguration)
                .isNotNull()
                .hasName("test")
                .hasWorkerCount(25)
                .hasPollIntervalInSeconds(5)
                .hasServerTimeoutPollIntervalMultiplicand(10)
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3)
                .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }

    @Test
    void dashboardWebServerConfigurationIsNotSetupWhenNotConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNull();
    }

    @Test
    void dashboardWebServerConfigurationIsSetupWhenConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.dashboardWebServerConfiguration()).isNotNull();
    }

    @Test
    void dashboardWebServerIsNotSetupWhenNotConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNull();
    }

    @Test
    void dashboardWebServerIsSetupWhenConfigured() {
        when(dashboardRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrProducer.dashboardWebServer(storageProvider, jsonMapper, usingStandardDashboardConfiguration())).isNotNull();
    }
}
