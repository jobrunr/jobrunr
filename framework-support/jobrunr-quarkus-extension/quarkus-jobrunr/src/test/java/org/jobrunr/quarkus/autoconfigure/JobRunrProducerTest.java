package org.jobrunr.quarkus.autoconfigure;

import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;

@ExtendWith(MockitoExtension.class)
class JobRunrProducerTest {

    JobRunrProducer jobRunrProducer;

    @Mock
    JobRunrRuntimeConfiguration jobRunrRuntimeConfiguration;
    @Mock
    JobRunrRuntimeConfiguration.JobSchedulerConfiguration jobSchedulerRunTimeConfiguration;
    @Mock
    StorageProvider storageProvider;

    @BeforeEach
    void setUp() {
        when(jobRunrRuntimeConfiguration.jobScheduler()).thenReturn(jobSchedulerRunTimeConfiguration);

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
}
