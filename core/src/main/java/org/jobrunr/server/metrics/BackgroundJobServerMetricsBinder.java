package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class BackgroundJobServerMetricsBinder implements AutoCloseable {

    private final BackgroundJobServer backgroundJobServer;
    private final MeterRegistry meterRegistry;
    private final List<Meter> meters;

    public BackgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        this.backgroundJobServer = backgroundJobServer;
        this.meterRegistry = meterRegistry;
        this.meters = new ArrayList<>();
        registerBackgroundJobServerMetrics();
    }

    public void registerBackgroundJobServerMetrics() {
        meters.add(registerFunction("poll-interval-in-seconds", bgJobServer -> (double) bgJobServer.getServerStatus().getPollIntervalInSeconds()));
        meters.add(registerFunction("worker-pool-size", bgJobServer -> (double) bgJobServer.getServerStatus().getWorkerPoolSize()));

        meters.add(registerGauge("process-all-located-memory", bgJobServer -> (double) bgJobServer.getServerStatus().getProcessAllocatedMemory()));
        meters.add(registerGauge("process-free-memory", bgJobServer -> (double) bgJobServer.getServerStatus().getProcessFreeMemory()));
        meters.add(registerGauge("system-free-memory", bgJobServer -> (double) bgJobServer.getServerStatus().getSystemFreeMemory()));
        meters.add(registerGauge("system-total-memory", bgJobServer -> (double) bgJobServer.getServerStatus().getSystemTotalMemory()));
        meters.add(registerGauge("first-heartbeat", bgJobServer -> (double) bgJobServer.getServerStatus().getFirstHeartbeat().getEpochSecond()));
        meters.add(registerGauge("last-heartbeat", bgJobServer -> (double) bgJobServer.getServerStatus().getLastHeartbeat().getNano()));
        meters.add(registerGauge("system-cpu-load", bgJobServer -> bgJobServer.getServerStatus().getSystemCpuLoad()));
        meters.add(registerGauge("process-cpu-load", bgJobServer -> bgJobServer.getServerStatus().getProcessCpuLoad()));
    }

    private FunctionCounter registerFunction(String name, ToDoubleFunction<BackgroundJobServer> func) {
        return FunctionCounter.builder(toMicroMeterName(name), this.backgroundJobServer, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
    }

    private Gauge registerGauge(String name, ToDoubleFunction<BackgroundJobServer> func) {
        return Gauge.builder(toMicroMeterName(name), this.backgroundJobServer, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
    }

    private String toMicroMeterName(String name) {
        return "jobrunr.background-job-server." + name;
    }

    @Override
    public void close() {
        meters.forEach(meter -> {
            try {
                meter.close();
            } catch (Exception e) {
                // nothing more we can do
            }
        });
    }
}
