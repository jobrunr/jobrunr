package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;


public class TaskStatistics {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskStatistics.class);

    private final DashboardNotificationManager dashboardNotificationManager;
    private long runCounter;
    private int exceptionCounter;
    private int runTookToLongCounter;

    public TaskStatistics(DashboardNotificationManager dashboardNotificationManager) {
        this.dashboardNotificationManager = dashboardNotificationManager;
        this.runCounter = 0L;
        this.exceptionCounter = 0;
        this.runTookToLongCounter = 0;
    }

    public PeriodicTaskRunInfo startRun(BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        return new PeriodicTaskRunInfo(this, backgroundJobServerConfiguration, ++runCounter);
    }

    public void handleException(Exception e) {
        exceptionCounter++;
        dashboardNotificationManager.handle(e);
    }

    public boolean hasTooManyExceptions() {
        return exceptionCounter > 5;
    }

    void logRun(long runIndex, boolean runSucceeded, Duration pollInterval, Instant runStartTime, Instant runEndTime) {
        if (runSucceeded && exceptionCounter > 0) {
            --exceptionCounter;
        }
        Duration actualRunDuration = Duration.between(runStartTime, runEndTime);
        if (actualRunDuration.getSeconds() < pollInterval.getSeconds()) {
            LOGGER.debug("JobZooKeeper run took {}", actualRunDuration);
            runTookToLongCounter = 0;
        } else {
            LOGGER.debug("JobZooKeeper run took {} (while pollIntervalInSeconds is {})", actualRunDuration, pollInterval);
            if (runTookToLongCounter < 2) {
                runTookToLongCounter++;
            } else {
                dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification(runIndex, (int) pollInterval.getSeconds(), runStartTime, (int) actualRunDuration.getSeconds()));
                runTookToLongCounter = 0;
            }
        }
    }
}