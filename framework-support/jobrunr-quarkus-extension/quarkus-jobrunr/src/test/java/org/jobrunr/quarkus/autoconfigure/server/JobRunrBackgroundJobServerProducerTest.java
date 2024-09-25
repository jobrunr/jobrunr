package org.jobrunr.quarkus.autoconfigure.server;

import org.assertj.core.api.Assertions;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;
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

        jobRunrBackgroundJobServerProducer = new JobRunrBackgroundJobServerProducer();
        setInternalState(jobRunrBackgroundJobServerProducer, "jobRunrRuntimeConfiguration", jobRunrRuntimeConfiguration);
    }

    @Test
    void backgroundJobServerConfigurationIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNull();
    }

    @Test
    void backgroundJobServerConfigurationIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(mock(BackgroundJobServerWorkerPolicy.class))).isNotNull();
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

        final BackgroundJobServerConfiguration backgroundJobServerConfiguration = jobRunrBackgroundJobServerProducer.backgroundJobServerConfiguration(jobRunrBackgroundJobServerProducer.backgroundJobServerWorkerPolicy());
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
        Assertions.assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "deleteSucceededJobsAfter")).isEqualTo(Duration.of(1, HOURS));
        Assertions.assertThat((Duration) getInternalState(backgroundJobServerConfiguration, "permanentlyDeleteDeletedJobsAfter")).isEqualTo(Duration.of(1, DAYS));
    }

    @Test
    void backgroundJobServerIsNotSetupWhenNotConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(false);

        Assertions.assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNull();
    }

    @Test
    void backgroundJobServerIsSetupWhenConfigured() {
        when(backgroundJobServerRunTimeConfiguration.enabled()).thenReturn(true);

        Assertions.assertThat(jobRunrBackgroundJobServerProducer.backgroundJobServer(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration())).isNotNull();
    }
}