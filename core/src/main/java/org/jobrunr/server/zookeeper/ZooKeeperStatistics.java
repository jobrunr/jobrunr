package org.jobrunr.server.zookeeper;

import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.BackgroundJobServerStatus;

import java.time.Duration;
import java.time.Instant;


public class ZooKeeperStatistics {

    private final DashboardNotificationManager dashboardNotificationManager;
    private long runCounter;
    private int exceptionCount;

    public ZooKeeperStatistics(DashboardNotificationManager dashboardNotificationManager) {
        this.dashboardNotificationManager = dashboardNotificationManager;
        this.runCounter = 0L;
        this.exceptionCount = 0;
    }

    public ZooKeeperRunTaskInfo startRun(BackgroundJobServerStatus backgroundJobServerStatus) {
        return new ZooKeeperRunTaskInfo(this, backgroundJobServerStatus, ++runCounter);
    }

    public void handleException(Exception e) {
        exceptionCount++;
        dashboardNotificationManager.handle(e);
    }

    public boolean hasTooManyExceptions() {
        return exceptionCount > 5;
    }

    void logRun(long runIndex, boolean runSucceeded, int pollIntervalInSeconds, Instant runStartTime, Instant runEndTime) {
        if(runSucceeded && exceptionCount > 0) {
            --exceptionCount;
        }
        Duration actualRunDuration = Duration.between(runStartTime, runEndTime);
        if(actualRunDuration.getSeconds() > pollIntervalInSeconds) {
            dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, pollIntervalInSeconds, runStartTime, (int) actualRunDuration.getSeconds()));
        }
    }
}
