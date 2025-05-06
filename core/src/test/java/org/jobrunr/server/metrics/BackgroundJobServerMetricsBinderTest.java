package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.cumulative.CumulativeFunctionCounter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jobrunr.JobRunrAssertions;
import org.jobrunr.server.BackgroundJobServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static java.util.UUID.randomUUID;
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
        }
    }

    @Test
    void getMetersReturnsBackgroundJobServerMetricsWhenBackgroundJobServerIsRunning() {
        var firstHeartBeat = Instant.now().minusSeconds(60);
        var lastHeartBeat = Instant.now();
        var serverStatus = aDefaultBackgroundJobServerStatus()
                .withId(backgroundJobServer.getId())
                .withFirstHeartbeat(firstHeartBeat)
                .withLastHeartbeat(lastHeartBeat)
                .build();
        when(backgroundJobServer.getServerStatus()).thenReturn(serverStatus);
        when(backgroundJobServer.isRunning()).thenReturn(true);
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
        when(backgroundJobServer.isRunning()).thenReturn(false);
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