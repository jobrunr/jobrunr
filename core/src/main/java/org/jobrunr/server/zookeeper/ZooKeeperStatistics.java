package org.jobrunr.server.zookeeper;

import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;


public class ZooKeeperStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZooKeeperStatistics.class);

    private final String zooKeeperName;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final Duration pollIntervalDuration;
    private long runCounter;
    private int exceptionCounter;
    private int runTookToLongCounter;

    public ZooKeeperStatistics(String zooKeeperName, int pollIntervalInSeconds, DashboardNotificationManager dashboardNotificationManager) {
        this.zooKeeperName = zooKeeperName;
        this.dashboardNotificationManager = dashboardNotificationManager;
        this.pollIntervalDuration = Duration.ofSeconds(pollIntervalInSeconds);
        this.runCounter = 0L;
        this.exceptionCounter = 0;
        this.runTookToLongCounter = 0;
    }

    public ZooKeeperRunTaskInfo startRun() {
        return new ZooKeeperRunTaskInfo(this, pollIntervalDuration, ++runCounter);
    }

    public void handleException(Exception e) {
        exceptionCounter++;
        dashboardNotificationManager.handle(e);
    }

    public boolean hasTooManyExceptions() {
        return exceptionCounter > 5;
    }

    void logRun(long runIndex, boolean runSucceeded, Duration pollIntervalDuration, Instant runStartTime, Instant runEndTime) {
        if (runSucceeded && exceptionCounter > 0) {
            --exceptionCounter;
        }
        Duration actualRunDuration = Duration.between(runStartTime, runEndTime);
        if (actualRunDuration.compareTo(pollIntervalDuration) < 0) {
            LOGGER.debug("{} run took {}", zooKeeperName, actualRunDuration);
            runTookToLongCounter = 0;
        } else {
            LOGGER.debug("{} run took {} (while pollIntervalDuration is {})", zooKeeperName, actualRunDuration, pollIntervalDuration);
            if (runTookToLongCounter < 2) {
                runTookToLongCounter++;
            } else {
                dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, (int) pollIntervalDuration.getSeconds(), runStartTime, (int) actualRunDuration.getSeconds()));
                runTookToLongCounter = 0;
            }
        }
    }
}
