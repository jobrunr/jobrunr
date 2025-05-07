package org.jobrunr.server.metrics;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.utils.resilience.CachedValue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

public class BackgroundJobServerMetricsBinder implements AutoCloseable {

    private final BackgroundJobServer backgroundJobServer;
    private final MeterRegistry meterRegistry;
    private final List<Meter> meters;
    private final CachedValue<BackgroundJobServerStatus> backgroundJobServerStatusCachedValue;

    public BackgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry) {
        this(backgroundJobServer, meterRegistry, Duration.ofSeconds(1));
    }

    public BackgroundJobServerMetricsBinder(BackgroundJobServer backgroundJobServer, MeterRegistry meterRegistry, Duration serverStatusTTL) {
        this.backgroundJobServer = backgroundJobServer;
        this.meterRegistry = meterRegistry;
        this.meters = new ArrayList<>();
        this.backgroundJobServerStatusCachedValue = new CachedValue<>(backgroundJobServer::getServerStatus, serverStatusTTL);
        registerBackgroundJobServerMetrics();
    }

    public void registerBackgroundJobServerMetrics() {
        meters.add(registerFunction("poll-interval-in-seconds", bgJobServerStatus -> (double) bgJobServerStatus.get().getPollIntervalInSeconds()));
        meters.add(registerFunction("worker-pool-size", bgJobServerStatus -> (double) bgJobServerStatus.get().getWorkerPoolSize()));

        meters.add(registerGauge("process-all-located-memory", bgJobServerStatus -> (double) bgJobServerStatus.get().getProcessAllocatedMemory()));
        meters.add(registerGauge("process-free-memory", bgJobServerStatus -> (double) bgJobServerStatus.get().getProcessFreeMemory()));
        meters.add(registerGauge("system-free-memory", bgJobServerStatus -> (double) bgJobServerStatus.get().getSystemFreeMemory()));
        meters.add(registerGauge("system-total-memory", bgJobServerStatus -> (double) bgJobServerStatus.get().getSystemTotalMemory()));
        meters.add(registerGauge("first-heartbeat", bgJobServerStatus -> (double) bgJobServerStatus.get().getFirstHeartbeat().getEpochSecond()));
        meters.add(registerGauge("last-heartbeat", bgJobServerStatus -> (double) bgJobServerStatus.get().getLastHeartbeat().getEpochSecond()));
        meters.add(registerGauge("system-cpu-load", bgJobServerStatus -> bgJobServerStatus.get().getSystemCpuLoad()));
        meters.add(registerGauge("process-cpu-load", bgJobServerStatus -> bgJobServerStatus.get().getProcessCpuLoad()));
    }

    private FunctionCounter registerFunction(String name, ToDoubleFunction<CachedValue<BackgroundJobServerStatus>> func) {
        return FunctionCounter.builder(toMicroMeterName(name), this.backgroundJobServerStatusCachedValue, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
    }

    private Gauge registerGauge(String name, ToDoubleFunction<CachedValue<BackgroundJobServerStatus>> func) {
        return Gauge.builder(toMicroMeterName(name), this.backgroundJobServerStatusCachedValue, func).tag("id", this.backgroundJobServer.getId().toString()).register(meterRegistry);
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
