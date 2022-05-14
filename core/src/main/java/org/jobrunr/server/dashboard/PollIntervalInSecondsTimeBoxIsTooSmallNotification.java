package org.jobrunr.server.dashboard;

public class PollIntervalInSecondsTimeBoxIsTooSmallNotification implements DashboardNotification {

    private final Long amountOfSeconds;

    public PollIntervalInSecondsTimeBoxIsTooSmallNotification(Long amountOfSeconds) {
        this.amountOfSeconds = amountOfSeconds;
    }

    public Long getAmountOfSeconds() {
        return amountOfSeconds;
    }
}
