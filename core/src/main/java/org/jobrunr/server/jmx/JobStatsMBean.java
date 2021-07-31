package org.jobrunr.server.jmx;

import java.time.Instant;

public interface JobStatsMBean {

    Instant getTimeStamp();

    Long getTotal();

    Long getScheduled();

    Long getEnqueued();

    Long getProcessing();

    Long getFailed();

    Long getSucceeded();

    int getRecurringJobs();

    int getBackgroundJobServers();
}
