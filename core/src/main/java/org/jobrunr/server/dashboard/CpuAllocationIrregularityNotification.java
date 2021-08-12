package org.jobrunr.server.dashboard;

public class CpuAllocationIrregularityNotification implements DashboardNotification {

    private final Integer amountOfSeconds;

    public CpuAllocationIrregularityNotification(Integer amountOfSeconds) {
        this.amountOfSeconds = amountOfSeconds;
    }

    public Integer getAmountOfSeconds() {
        return amountOfSeconds;
    }
}
