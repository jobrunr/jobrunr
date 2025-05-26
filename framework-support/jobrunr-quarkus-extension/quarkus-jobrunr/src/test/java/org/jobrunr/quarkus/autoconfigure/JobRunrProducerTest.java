package org.jobrunr.quarkus.autoconfigure;

import org.jobrunr.server.carbonaware.CarbonAwareConfigurationReader;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.jobrunr.jobs.carbonaware.CarbonAwareConfigurationAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrProducerTest {

    JobRunrProducer jobRunrProducer;
    @Mock
    JobRunrRuntimeConfiguration.JobsConfiguration jobsRunTimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobSchedulerConfiguration jobSchedulerRunTimeConfiguration;
    @Mock
    StorageProvider storageProvider;
    @Mock
    JobRunrRuntimeConfiguration.CarbonAwareConfiguration carbonAwareRunTimeConfiguration;
    @Mock
    JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        lenient().when(jobRunrRuntimeConfiguration.jobs()).thenReturn(jobsRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobs().carbonAwareConfiguration()).thenReturn(carbonAwareRunTimeConfiguration);
        lenient().when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);

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
    void carbonAwareJobManagerIsSetupWhenConfigured() {
        when(carbonAwareRunTimeConfiguration.isEnabled()).thenReturn(true);
        when(carbonAwareRunTimeConfiguration.areaCode()).thenReturn(Optional.of("DE"));
        when(carbonAwareRunTimeConfiguration.carbonIntensityApiUrl()).thenReturn(Optional.of("http://carbon.be"));
        when(carbonAwareRunTimeConfiguration.apiClientConnectTimeoutMs()).thenReturn(Optional.of(500));
        when(carbonAwareRunTimeConfiguration.apiClientReadTimeoutMs()).thenReturn(Optional.of(1000));

        CarbonAwareJobManager carbonAwareJobManager = jobRunrProducer.carbonAwareJobManager(jsonMapper);
        CarbonAwareConfigurationReader carbonAwareConfiguration = getInternalState(carbonAwareJobManager, "carbonAwareConfiguration");

        assertThat(carbonAwareConfiguration)
                .hasAreaCode("DE")
                .hasCarbonAwareApiUrl("http://carbon.be")
                .hasApiClientConnectTimeout(Duration.ofMillis(500))
                .hasApiClientReadTimeout(Duration.ofMillis(1000));
    }

    @Test
    void carbonAwareJobManagerIsSetupWhenConfiguredWithExternalCode() {
        when(carbonAwareRunTimeConfiguration.isEnabled()).thenReturn(true);
        when(carbonAwareRunTimeConfiguration.externalCode()).thenReturn(Optional.of("external"));
        when(carbonAwareRunTimeConfiguration.dataProvider()).thenReturn(Optional.of("provider"));

        CarbonAwareJobManager carbonAwareJobManager = jobRunrProducer.carbonAwareJobManager(jsonMapper);
        CarbonAwareConfigurationReader carbonAwareConfiguration = getInternalState(carbonAwareJobManager, "carbonAwareConfiguration");

        assertThat(carbonAwareConfiguration)
                .hasExternalCode("external")
                .hasDataProvider("provider");
    }
}
