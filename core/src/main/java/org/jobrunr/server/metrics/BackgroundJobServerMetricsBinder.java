package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;

import java.util.function.ToDoubleFunction;

public class BackgroundJobServerMetricsBinder {

    private final BackgroundJobServer backgroundJobServer;
    private final MeterRegistry meterRegistry;

    public BackgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        this.backgroundJobServer = backgroundJobServer;
        this.meterRegistry = meterRegistry;
        registerBackgroundJobServerMetrics();
    }

    public void registerBackgroundJobServerMetrics() {
        registerFunction("poll-interval-in-seconds", (bgJobServer) -> (double) bgJobServer.getServerStatus().getPollIntervalInSeconds());
        registerFunction("worker-pool-size", (bgJobServer) -> (double) bgJobServer.getServerStatus().getWorkerPoolSize());

        registerGauge("process-all-located-memory", (bgJobServer) -> (double) bgJobServer.getServerStatus().getProcessAllocatedMemory());
        registerGauge("process-free-memory", (bgJobServer) -> (double) bgJobServer.getServerStatus().getProcessFreeMemory());
        registerGauge("system-free-memory", (bgJobServer) -> (double) bgJobServer.getServerStatus().getSystemFreeMemory());
        registerGauge("system-total-memory", (bgJobServer) -> (double) bgJobServer.getServerStatus().getSystemTotalMemory());
        registerGauge("first-heartbeat", (bgJobServer) -> (double) bgJobServer.getServerStatus().getFirstHeartbeat().getEpochSecond());
        registerGauge("last-heartbeat", (bgJobServer) -> (double) bgJobServer.getServerStatus().getLastHeartbeat().getNano());
    }

    private <T> void registerFunction(String name, ToDoubleFunction<BackgroundJobServer> func) {
        FunctionCounter.builder(toMicroMeterName(name), this.backgroundJobServer, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
    }

    private <T> void registerGauge(String name, ToDoubleFunction<BackgroundJobServer> func) {
        Gauge.builder(toMicroMeterName(name), this.backgroundJobServer, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
    }

    private String toMicroMeterName(String name) {
        return "jobrunr.background-job-server." + name;
    }
}
