package org.jobrunr.quarkus.autoconfigure.server;

import org.jobrunr.jobs.carbonaware.CarbonAwareJobProcessingConfigurationAssert;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.JobActivator;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
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
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrBackgroundJobServerProducerTest {

    JobRunrBackgroundJobServerProducer jobRunrBackgroundJobServerProducer;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
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
    JobRunrRuntimeConfiguration.CarbonAwareJobProcessingConfiguration carbonAwareJobProcessingRunTimeConfiguration;

    @Mock
    StorageProvider storageProvider;

    @Mock
    JsonMapper jsonMapper;

    @Mock
    JobActivator jobActivator;

    @BeforeEach
    void setUp() {
        lenient().when(jobRunrRuntimeConfiguration.jobs()).thenReturn(jobsRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.backgroundJobServer()).thenReturn(backgroundJobServerRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.dashboard()).thenReturn(dashboardRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.miscellaneous()).thenReturn(miscellaneousRunTimeConfiguration);
        lenient().when(backgroundJobServerRunTimeConfiguration.carbonAwareJobProcessingConfiguration()).thenReturn(carbonAwareJobProcessingRunTimeConfiguration);

        jobRunrBackgroundJobServerProducer = new JobRunrBackgroundJobServerProducer();
        setInternalState(jobRunrBackgroundJobServerProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(false);

        assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNotNull();
    }

    @Test
    void backgroundJobServerConfigurationMapsCorrectConfigurationWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        when(backgroundJobServerRunTimeConfiguration.name()).thenReturn(Optional.of("test"));
        when(backgroundJobServerRunTimeConfiguration.workerCount()).thenReturn(Optional.of(25));
        when(backgroundJobServerRunTimeConfiguration.threadType()).thenReturn(Optional.of(BackgroundJobServerThreadType.PlatformThreads));
        when(backgroundJobServerRunTimeConfiguration.pollIntervalInSeconds()).thenReturn(Optional.of(5));
        when(backgroundJobServerRunTimeConfiguration.getCarbonAwareJobProcessingPollIntervalInMinutes()).thenReturn(Optional.of(15));
        when(backgroundJobServerRunTimeConfiguration.serverTimeoutPollIntervalMultiplicand()).thenReturn(Optional.of(10));
        when(backgroundJobServerRunTimeConfiguration.scheduledJobsRequestSize()).thenReturn(Optional.of(1));
        when(backgroundJobServerRunTimeConfiguration.orphanedJobsRequestSize()).thenReturn(Optional.of(2));
        when(backgroundJobServerRunTimeConfiguration.succeededJobRequestSize()).thenReturn(Optional.of(3));
        when(backgroundJobServerRunTimeConfiguration.deleteSucceededJobsAfter()).thenReturn(Optional.of(Duration.of(1, HOURS)));
        when(backgroundJobServerRunTimeConfiguration.permanentlyDeleteDeletedJobsAfter()).thenReturn(Optional.of(Duration.of(1, DAYS)));
        when(backgroundJobServerRunTimeConfiguration.interruptJobsAwaitDurationOnStop()).thenReturn(Optional.of(Duration.of(20, SECONDS)));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(jobRunrBackgroundJobServerProducer.backgroundJobServerWorkerPolicy());
        assertThat(backgroundJobServerConfiguration)
                .isNotNull()
                .hasName("test")
                .hasWorkerCount(25)
                .hasPollIntervalInSeconds(5)
                .hasCarbonAwareJobProcessingPollIntervalInMinutes(15)
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

        assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }

    @Test
    void carbonAwareJobProcessingIsSetupWhenConfiguredAndBackgroundJobServerIsEnabled() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        when(carbonAwareJobProcessingRunTimeConfiguration.isEnabled()).thenReturn(true);
        when(carbonAwareJobProcessingRunTimeConfiguration.areaCode()).thenReturn(Optional.of("DE"));
        when(carbonAwareJobProcessingRunTimeConfiguration.apiClientConnectTimeoutMs()).thenReturn(Optional.of(500));
        when(carbonAwareJobProcessingRunTimeConfiguration.apiClientReadTimeoutMs()).thenReturn(Optional.of(1000));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(jobRunrBackgroundJobServerProducer.backgroundJobServerWorkerPolicy());
        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = new BackgroundJobServerConfigurationReader(backgroundJobServerConfiguration).getCarbonAwareJobProcessingConfiguration();

        assertThat(carbonAwareJobProcessingConfiguration)
                .hasAreaCode("DE")
                .hasApiClientConnectTimeout(Duration.ofMillis(500))
                .hasApiClientReadTimeout(Duration.ofMillis(1000));
    }

    @Test
    void carbonAwareJobProcessingIsSetupWhenConfiguredWithExternalCodeAndBackgroundJobServerIsEnabled() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        when(carbonAwareJobProcessingRunTimeConfiguration.isEnabled()).thenReturn(true);
        when(carbonAwareJobProcessingRunTimeConfiguration.externalCode()).thenReturn(Optional.of("external"));
        when(carbonAwareJobProcessingRunTimeConfiguration.dataProvider()).thenReturn(Optional.of("provider"));

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(jobRunrBackgroundJobServerProducer.backgroundJobServerWorkerPolicy());
        CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration = new BackgroundJobServerConfigurationReader(backgroundJobServerConfiguration).getCarbonAwareJobProcessingConfiguration();

        CarbonAwareJobProcessingConfigurationAssert.assertThat(carbonAwareJobProcessingConfiguration)
                .hasExternalCode("external")
                .hasDataProvider("provider");
    }
}