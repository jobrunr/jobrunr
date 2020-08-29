package org.jobrunr.storage;

import java.time.Instant;

public class JobStatsExtended extends JobStats {

    private final Long amountSucceeded;
    private final Long amountFailed;
    private final Estimation estimation;

    public JobStatsExtended(JobStats jobStats) {
        super(jobStats);
        this.amountSucceeded = 0L;
        this.amountFailed = 0L;
        this.estimation = new Estimation(jobStats.getEnqueued() < 1 && jobStats.getProcessing() < 1);
    }

    public JobStatsExtended(JobStats jobStats, Long amountSucceeded, Long amountFailed, Instant estimatedProcessingFinishedInstant) {
        super(jobStats);
        this.amountSucceeded = amountSucceeded;
        this.amountFailed = amountFailed;
        this.estimation = new Estimation(jobStats.getEnqueued() < 1 && jobStats.getProcessing() < 1, estimatedProcessingFinishedInstant);
    }

    public Long getAmountSucceeded() {
        return amountSucceeded;
    }

    public Long getAmountFailed() {
        return amountFailed;
    }

    public Estimation getEstimation() {
        return estimation;
    }

    public static class Estimation {
        private final boolean processingDone;
        private final boolean estimatedProcessingTimeAvailable;
        private final Instant estimatedProcessingFinishedAt;

        public Estimation(boolean processingDone) {
            this.processingDone = processingDone;
            this.estimatedProcessingTimeAvailable = false;
            this.estimatedProcessingFinishedAt = null;
        }

        public Estimation(boolean processingDone, Instant estimatedProcessingFinishedAt) {
            this.processingDone = processingDone;
            this.estimatedProcessingTimeAvailable = estimatedProcessingFinishedAt != null;
            this.estimatedProcessingFinishedAt = estimatedProcessingFinishedAt;
        }

        public Instant getEstimatedProcessingFinishedAt() {
            return estimatedProcessingFinishedAt;
        }

        public boolean isProcessingDone() {
            return processingDone;
        }

        public boolean isEstimatedProcessingFinishedInstantAvailable() {
            return estimatedProcessingTimeAvailable;
        }
    }
}
