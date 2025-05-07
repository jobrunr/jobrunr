package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.internal.DefaultGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServerMetricsBinderTest {

    @Mock
    BackgroundJobServer backgroundJobServer;
    @Spy
    MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        when(backgroundJobServer.getId()).thenReturn(randomUUID());
    }

    @Test
    void testBinder() {
        new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);

        verify(meterRegistry, times(2)).more();

        List<Meter> meters = meterRegistry.getMeters();
        assertThat(meters).hasSize(10);
    }

    @Test
    void testBinderCachesValues() {
        when(backgroundJobServer.getServerStatus()).thenReturn(aDefaultBackgroundJobServerStatus().build());
        
        new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);

        List<DefaultGauge> gauges = meterRegistry.getMeters().stream()
                .filter(DefaultGauge.class::isInstance)
                .map(DefaultGauge.class::cast)
                .collect(toList());

        assertThat(gauges)
                .hasSize(8)
                .allSatisfy(gauge -> {
                    Double value1 = gauge.value();
                    Double value2 = gauge.value();
                    assertThat(value1).isEqualTo(value2);
                });
    }

}