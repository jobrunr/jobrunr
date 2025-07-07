package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.cumulative.CumulativeFunctionCounter;
import io.micrometer.core.instrument.internal.DefaultGauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.JobRunrAssertions;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.jobrunr.stubs.Mocks.ofBackgroundJobServer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobServerMetricsBinderTest {

    BackgroundJobServer backgroundJobServer = ofBackgroundJobServer();
    SimpleMeterRegistry simpleMeterRegistry = Mockito.spy(new SimpleMeterRegistry());

    @BeforeEach
    void setUp() {
        when(backgroundJobServer.getId()).thenReturn(randomUUID());
    }

    @Test
    void testBinder() {
        try (var ignored = new BackgroundJobServerMetricsBinder(backgroundJobServer, simpleMeterRegistry)) {
            verify(simpleMeterRegistry, times(2)).more();

            List<Meter> meters = simpleMeterRegistry.getMeters();
            assertThat(meters).hasSize(10);
        }
    }

    @Test
    void testBinderCachesValues() {
        when(backgroundJobServer.getServerStatus()).thenReturn(aDefaultBackgroundJobServerStatus().build());

        try (var ignored = new BackgroundJobServerMetricsBinder(backgroundJobServer, simpleMeterRegistry)) {
            List<DefaultGauge> gauges = simpleMeterRegistry.getMeters().stream()
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

    @Test
    void getMetersReturnsBackgroundJobServerMetricsWhenBackgroundJobServerIsRunning() {
        var firstHeartBeat = now().minusSeconds(60);
        var lastHeartBeat = now();
        var serverStatus = aDefaultBackgroundJobServerStatus()
                .withId(backgroundJobServer.getId())
                .withFirstHeartbeat(firstHeartBeat)
                .withLastHeartbeat(lastHeartBeat)
                .withRunning(true)
                .build();
        when(backgroundJobServer.getServerStatus()).thenReturn(serverStatus);
        try (var ignored1 = new BackgroundJobServerMetricsBinder(backgroundJobServer, simpleMeterRegistry)) {

            // WHEN
            List<Meter> meters = simpleMeterRegistry.getMeters();

            // THEN
            assertThat(meters).hasSize(10);

            meters.forEach(meter -> JobRunrAssertions.assertThat(meter).hasIdWithTag("id", serverStatus.getId().toString()));

            assertThat(getCounter(meters, "jobrunr.background-job-server.poll-interval-in-seconds").count()).isEqualTo(serverStatus.getPollIntervalInSeconds());
            assertThat(getCounter(meters, "jobrunr.background-job-server.worker-pool-size").count()).isEqualTo(serverStatus.getWorkerPoolSize());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-all-located-memory").value()).isEqualTo((double) serverStatus.getProcessAllocatedMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-free-memory").value()).isEqualTo((double) serverStatus.getProcessFreeMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-free-memory").value()).isEqualTo((double) serverStatus.getSystemFreeMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-total-memory").value()).isEqualTo((double) serverStatus.getSystemTotalMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.first-heartbeat").value()).isEqualTo(firstHeartBeat.getEpochSecond());
            assertThat(getGauge(meters, "jobrunr.background-job-server.last-heartbeat").value()).isEqualTo(lastHeartBeat.getEpochSecond());
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-cpu-load").value()).isEqualTo((double) serverStatus.getSystemCpuLoad());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-cpu-load").value()).isEqualTo((double) serverStatus.getProcessCpuLoad());
        }
    }

    @Test
    void getMetersReturnsBackgroundJobServerMetricsWhenBackgroundJobServerIsNotRunning() {
        var serverStatus = aDefaultBackgroundJobServerStatus()
                .withId(backgroundJobServer.getId())
                .withFirstHeartbeat(null)
                .withLastHeartbeat(null)
                .build();
        when(backgroundJobServer.getServerStatus()).thenReturn(serverStatus);
        try (var ignored = new BackgroundJobServerMetricsBinder(backgroundJobServer, simpleMeterRegistry)) {

            // WHEN
            List<Meter> meters = simpleMeterRegistry.getMeters();

            // THEN
            assertThat(meters).hasSize(10);

            meters.forEach(meter -> JobRunrAssertions.assertThat(meter).hasIdWithTag("id", serverStatus.getId().toString()));

            assertThat(getCounter(meters, "jobrunr.background-job-server.poll-interval-in-seconds").count()).isEqualTo(serverStatus.getPollIntervalInSeconds());
            assertThat(getCounter(meters, "jobrunr.background-job-server.worker-pool-size").count()).isEqualTo(serverStatus.getWorkerPoolSize());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-all-located-memory").value()).isEqualTo((double) serverStatus.getProcessAllocatedMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-free-memory").value()).isEqualTo((double) serverStatus.getProcessFreeMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-free-memory").value()).isEqualTo((double) serverStatus.getSystemFreeMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-total-memory").value()).isEqualTo((double) serverStatus.getSystemTotalMemory());
            assertThat(getGauge(meters, "jobrunr.background-job-server.first-heartbeat").value()).isEqualTo(-1);
            assertThat(getGauge(meters, "jobrunr.background-job-server.last-heartbeat").value()).isEqualTo(-1);
            assertThat(getGauge(meters, "jobrunr.background-job-server.system-cpu-load").value()).isEqualTo((double) serverStatus.getSystemCpuLoad());
            assertThat(getGauge(meters, "jobrunr.background-job-server.process-cpu-load").value()).isEqualTo((double) serverStatus.getProcessCpuLoad());
        }
    }

    private static CumulativeFunctionCounter<?> getCounter(List<Meter> meters, String name) {
        return (CumulativeFunctionCounter<?>) meters.stream().filter(m -> name.equals(m.getId().getName())).findFirst().orElseThrow();
    }

    private static Gauge getGauge(List<Meter> meters, String name) {
        return (Gauge) meters.stream().filter(m -> name.equals(m.getId().getName())).findFirst().orElseThrow();
    }
}