package org.jobrunr.server.jmx;

import java.time.Instant;
import java.util.UUID;

public interface BackgroundJobServerStatusMBean {
    UUID getId();

    int getWorkerPoolSize();

    Instant getFirstHeartbeat();

    Instant getLastHeartbeat();

    int getPollIntervalInSeconds();

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
