package org.jobrunr.server.jmx;

import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface BackgroundJobServerStatusMBean {
    UUID getId();

    String getName();

    int getWorkerPoolSize();

    int getPollIntervalInSeconds();

    @Nullable
    Instant getFirstHeartbeat();

    @Nullable
    Instant getLastHeartbeat();

    Duration getDeleteSucceededJobsAfter();

    Duration getPermanentlyDeleteDeletedJobsAfter();

    boolean isRunning();

    Long getSystemTotalMemory();

    Long getSystemFreeMemory();

    Double getSystemCpuLoad();

    Long getProcessMaxMemory();

    Long getProcessFreeMemory();

    Long getProcessAllocatedMemory();

    Double getProcessCpuLoad();
}
