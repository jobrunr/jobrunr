package org.jobrunr.server.tasks;

import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;

import static java.lang.String.valueOf;
import static java.time.Instant.now;

public class PeriodicTaskRunInfo extends TaskRunInfo implements AutoCloseable {

    private static final String RUN_COUNTER_MDC_KEY = "jobrunr.zooKeeper.runCounter";

    private final TaskStatistics taskStatistics;
    private final Duration pollIntervalTimeBoxDuration;

    private final long runIndex;
    private boolean runSucceeded;

    public PeriodicTaskRunInfo(TaskStatistics taskStatistics, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration, long runIndex) {
        super(backgroundJobServerConfiguration);
        this.taskStatistics = taskStatistics;
        this.pollIntervalTimeBoxDuration = getBackgroundJobServerConfiguration().getPollInterval().multipliedBy(95).dividedBy(100);
        this.runIndex = runIndex;
        this.runSucceeded = false;
        MDC.put(RUN_COUNTER_MDC_KEY, valueOf(runIndex));
    }

    @Override
    public boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration runTimeDuration = Duration.between(getRunStartTime(), now());
        return runTimeDuration.compareTo(pollIntervalTimeBoxDuration) >= 0;
    }

    public void markRunAsSucceeded() {
        this.runSucceeded = true;
    }

    @Override
    public void close() {
        MDC.remove(RUN_COUNTER_MDC_KEY);
        taskStatistics.logRun(runIndex, runSucceeded, getBackgroundJobServerConfiguration().getPollInterval(), getRunStartTime(), Instant.now());
    }
}
