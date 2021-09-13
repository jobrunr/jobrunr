package org.jobrunr.storage.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicLong;

import static org.jobrunr.storage.JobStats.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageProviderMetricsBinderTest {

    @Mock
    StorageProvider storageProvider;
    @Mock
    MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        when(storageProvider.getJobStats()).thenReturn(empty());
    }

    @Test
    void testBinder() {
        new StorageProviderMetricsBinder(storageProvider, meterRegistry);

        verify(meterRegistry, times(7)).gauge(anyString(), any(), any(AtomicLong.class));
    }

}