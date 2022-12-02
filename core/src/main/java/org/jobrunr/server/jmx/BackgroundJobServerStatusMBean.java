package org.jobrunr.server.jmx;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface BackgroundJobServerStatusMBean {
    UUID getId();

    String getName();

    int getWorkerPoolSize();

    int getPollIntervalInSeconds();

    Instant getFirstHeartbeat();

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
