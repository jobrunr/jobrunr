package org.jobrunr.quarkus.autoconfigure;

import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrProducerTest {

    JobRunrProducer jobRunrProducer;

    @Mock
    JobRunrBuildTimeConfiguration jobRunrBuildTimeConfiguration;
    @Mock
    JobRunrBuildTimeConfiguration.JobSchedulerConfiguration jobSchedulerBuildTimeConfiguration;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobSchedulerConfiguration jobSchedulerRunTimeConfiguration;
    @Mock
    StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        lenient().when(jobRunrBuildTimeConfiguration.jobScheduler()).thenReturn(jobSchedulerBuildTimeConfiguration);

        lenient().when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);

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
}
