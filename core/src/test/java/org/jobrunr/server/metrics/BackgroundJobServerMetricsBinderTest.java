package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServerMetricsBinderTest {

    @Mock
    BackgroundJobServer backgroundJobServer;
    @Mock
    MeterRegistry meterRegistry;
    @Mock
    MeterRegistry.More more;

    @BeforeEach
    void setUp() {
        when(backgroundJobServer.getId()).thenReturn(randomUUID());
        when(meterRegistry.more()).thenReturn(more);
    }

    @Test
    void testBinder() {
        new BackgroundJobServerMetricsBinder(backgroundJobServer, meterRegistry);

        verify(meterRegistry, times(2)).more();
    }

}