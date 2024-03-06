package org.jobrunr.server.zookeeper;

import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
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

    public ZooKeeperRunTaskInfo startRun(BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        return new ZooKeeperRunTaskInfo(this, backgroundJobServerConfiguration, ++runCounter);
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
            if (runTookToLongCounter < 2) {
                LOGGER.info("JobZooKeeper run took {} which exceeded the pollIntervalInSeconds of {} for {} times.", actualRunDuration, pollIntervalInSeconds, runTookToLongCounter + 1);
                runTookToLongCounter++;
            } else {
                LOGGER.warn("JobZooKeeper run took {} which exceeded the pollIntervalInSeconds of {} for {} times. Notifying dashboard.", actualRunDuration, pollIntervalInSeconds, runTookToLongCounter + 1);
                dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, pollIntervalInSeconds, runStartTime, (int) actualRunDuration.getSeconds()));
                runTookToLongCounter = 0;
            }
        }
    }
}
