package org.jobrunr.server.zookeeper;

import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;


public class ZooKeeperStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperStatistics.class);

    private final DashboardNotificationManager dashboardNotificationManager;
    private long runCounter;
    private int exceptionCounter;
    private int runTookToLongCounter;

    public ZooKeeperStatistics(DashboardNotificationManager dashboardNotificationManager) {
        this.dashboardNotificationManager = dashboardNotificationManager;
        this.runCounter = 0L;
        this.exceptionCounter = 0;
        this.runTookToLongCounter = 0;
    }

    public ZooKeeperRunTaskInfo startRun(BackgroundJobServerStatus backgroundJobServerStatus) {
        return new ZooKeeperRunTaskInfo(this, backgroundJobServerStatus, ++runCounter);
    }

    public void handleException(Exception e) {
        exceptionCounter++;
        dashboardNotificationManager.handle(e);
    }

    public boolean hasTooManyExceptions() {
        return exceptionCounter > 5;
    }

    void logRun(long runIndex, boolean runSucceeded, int pollIntervalInSeconds, Instant runStartTime, Instant runEndTime) {
        if (runSucceeded && exceptionCounter > 0) {
            --exceptionCounter;
        }
        Duration actualRunDuration = Duration.between(runStartTime, runEndTime);
        if (actualRunDuration.getSeconds() < pollIntervalInSeconds) {
            LOGGER.debug("JobZooKeeper run took {}", actualRunDuration);
            runTookToLongCounter = 0;
        } else {
            LOGGER.debug("JobZooKeeper run took {} (while pollIntervalInSeconds is {})", actualRunDuration, pollIntervalInSeconds);
            if (runTookToLongCounter < 2) {
                runTookToLongCounter++;
            } else {
                dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, pollIntervalInSeconds, runStartTime, (int) actualRunDuration.getSeconds()));
                runTookToLongCounter = 0;
            }
        }
    }
}
