package org.jobrunr.server.zookeeper;

import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class ZooKeeperStatistics {

    private final DashboardNotificationManager dashboardNotificationManager;
    private final AtomicLong runCounter;
    private final AtomicInteger exceptionCount;

    public ZooKeeperStatistics(DashboardNotificationManager dashboardNotificationManager) {
        this.dashboardNotificationManager = dashboardNotificationManager;
        this.runCounter = new AtomicLong();
        this.exceptionCount = new AtomicInteger();
    }

    public ZooKeeperRunTaskInfo startRun(BackgroundJobServerStatus backgroundJobServerStatus) {
        return new ZooKeeperRunTaskInfo(this, backgroundJobServerStatus, runCounter.incrementAndGet());
    }

    public void handleException(Exception e) {
        exceptionCount.getAndIncrement();
        dashboardNotificationManager.handle(e);
    }

    public boolean hasTooManyExceptions() {
        return exceptionCount.get() > 5;
    }

    void logRun(long runIndex, int pollIntervalInSeconds, Instant runStartTime, Instant runEndTime) {
        Duration actualRunDuration = Duration.between(runStartTime, runEndTime);
        if(actualRunDuration.getSeconds() > pollIntervalInSeconds) {
            dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, pollIntervalInSeconds, runStartTime, (int) actualRunDuration.getSeconds()));
        }
    }
}
