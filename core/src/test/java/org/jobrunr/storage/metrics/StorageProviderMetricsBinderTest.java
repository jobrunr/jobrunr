package org.jobrunr.storage.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
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
    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        when(storageProvider.getJobStats()).thenReturn(empty());
    }

    @Test
    void testBinder() {
        new StorageProviderMetricsBinder(storageProvider, meterRegistry);

        verify(meterRegistry, times(9)).gauge(anyString(), any(), any(AtomicLong.class));

        List<Gauge> meters = meterRegistry.getMeters().stream().map(Gauge.class::cast).collect(toList());
        assertThat(meters)
                .allSatisfy(meter -> {
                    Meter.Id id = meter.getId();
                    assertThat(id.getName()).isEqualTo("jobrunr.jobs.by-state");
                    assertThat(id.getTags())
                            .hasSize(1)
                            .allSatisfy(x -> assertThat(x.getKey()).isEqualTo("state"));
                });
    }

}