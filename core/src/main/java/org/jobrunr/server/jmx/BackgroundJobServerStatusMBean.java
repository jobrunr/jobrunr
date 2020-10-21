package org.jobrunr.server.jmx;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public interface BackgroundJobServerStatusMBean {
    UUID getId();

    int getWorkerPoolSize();

    int getPollIntervalInSeconds();

    Instant getFirstHeartbeat();

    Instant getLastHeartbeat();

    Duration getDeleteSucceededJobsAfter();

    Duration getPermanentlyDeleteDeletedJobsAfter();

    boolean isRunning();

    void start();

    void pause();

    void resume();

    void stop();

    Long getSystemTotalMemory();

    Long getSystemFreeMemory();

    Double getSystemCpuLoad();

    Long getProcessMaxMemory();

    Long getProcessFreeMemory();

    Long getProcessAllocatedMemory();

    Double getProcessCpuLoad();
}
