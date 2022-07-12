package org.jobrunr.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.metrics.StorageProviderMetricsBinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobRunrMicroMeterIntegrationTest {

    @Mock
    private MeterRegistry meterRegistry;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private BackgroundJobServer backgroundJobServer;

    @Test
    void testWithStorageProviderOnly() {
        // GIVEN
        JobRunrMicroMeterIntegration jobRunrMicroMeterIntegration = new JobRunrMicroMeterIntegration(meterRegistry);
        when(storageProvider.getJobStats()).thenReturn(JobStats.empty());

        // WHEN
        jobRunrMicroMeterIntegration.initialize(storageProvider, null);

        // THEN
        verify(storageProvider).getJobStats();
        verify(storageProvider).addJobStorageOnChangeListener(any(StorageProviderMetricsBinder.class));
        verify(meterRegistry, times(9)).gauge(any(String.class), any(Iterable.class), any(AtomicLong.class));

        // WHEN
        assertThatCode(jobRunrMicroMeterIntegration::close).doesNotThrowAnyException();
    }

    @Test
    void testWithStorageProviderAndBackgroundJobServerOnly() {
        // GIVEN
        JobRunrMicroMeterIntegration jobRunrMicroMeterIntegration = new JobRunrMicroMeterIntegration(meterRegistry);
        when(meterRegistry.more()).thenReturn(mock(MeterRegistry.More.class));
        when(storageProvider.getJobStats()).thenReturn(JobStats.empty());
        when(backgroundJobServer.getId()).thenReturn(UUID.randomUUID());

        // WHEN
        jobRunrMicroMeterIntegration.initialize(storageProvider, backgroundJobServer);

        // THEN
        verify(storageProvider).getJobStats();
        verify(storageProvider).addJobStorageOnChangeListener(any(StorageProviderMetricsBinder.class));
        verify(meterRegistry, times(9)).gauge(any(String.class), any(Iterable.class), any(AtomicLong.class));

        // WHEN
        assertThatCode(jobRunrMicroMeterIntegration::close).doesNotThrowAnyException();
    }

}