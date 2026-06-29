package org.jobrunr.server.jmx;

import org.jobrunr.storage.BackgroundJobServerStatusMetadata;

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

    // TODO I don't think this belongs here, as far as I know, the interface is used for JMX which doesn't need these info
    String getMetadataString();

    BackgroundJobServerStatusMetadata getMetadata();
}
