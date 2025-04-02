package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;

import java.util.Map;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobDashboardProgressBar {

    public static final String JOBRUNR_PROGRESSBAR_KEY = "jobRunrDashboardProgressBar";

    private final JobDashboardProgress jobDashboardProgress;

    public JobDashboardProgressBar(Job job, Long totalAmount) {
        this(initJobDashboardProgress(job, totalAmount));
    }

    public JobDashboardProgressBar(JobDashboardProgress jobDashboardProgress) {
        this.jobDashboardProgress = jobDashboardProgress;
    }

    public static JobDashboardProgressBar get(Job job) {
        Map<String, Object> jobMetadata = job.getMetadata();
        return jobMetadata.keySet().stream().filter(key -> key.startsWith(JOBRUNR_PROGRESSBAR_KEY))
                .max(String::compareTo)
                .map(key -> new JobDashboardProgressBar(cast(jobMetadata.get(key))))
                .orElse(null);
    }

    private static JobDashboardProgress initJobDashboardProgress(Job job, Long totalAmount) {
        Map<String, Object> jobMetadata = job.getMetadata();
        String progressBarKey = progressBarKey(job.getJobStates().size());
        jobMetadata.putIfAbsent(progressBarKey, new JobDashboardProgress(totalAmount));
        return cast(jobMetadata.get(progressBarKey));
    }

    /**
     * Allows to increase the progress bar in the dashboard for a normal job using the {@link JobContext}
     */
    public void incrementSucceeded() {
        jobDashboardProgress.incrementSucceeded();
    }

    /**
     * Allows to increase the failed count of the progress bar in the dashboard for a normal job using the {@link JobContext}
     */
    public void incrementFailed() {
        jobDashboardProgress.incrementFailed();
    }

    public int getProgress() {
        return jobDashboardProgress.getProgress();
    }

    public long getSucceededAmount() {
        return jobDashboardProgress.getSucceededAmount();
    }

    public long getFailedAmount() {
        return jobDashboardProgress.getFailedAmount();
    }

    public long getTotalAmount() {
        return jobDashboardProgress.getTotalAmount();
    }

    /**
     * Sets the progress for the ProgressBar on the dashboard and returns if it has changes.
     *
     * @param succeededAmount the amount of succeeded items
     * @return true if the progress has changed, false otherwise
     */
    public boolean setProgress(long succeededAmount) {
        return jobDashboardProgress.setProgress(succeededAmount);
    }

    public boolean setProgress(long totalAmount, long succeededAmount, long failedAmount) {
        return this.jobDashboardProgress.setProgress(totalAmount, succeededAmount, failedAmount);
    }

    /**
     * Returns a unique key based on the current jobState (so that the progressbar regarding the first processing attempt can be displayed under the first processing view in the dashboard, ... )
     *
     * @param jobStateNbr the current state nbr - typically enqueued=1, processing=2, failed=3, scheduled=4, enqueued=5, processing=6, ...
     * @return a progress bar key for the metadata matching the current job state.
     */
    private static String progressBarKey(int jobStateNbr) {
        return JOBRUNR_PROGRESSBAR_KEY + "-" + jobStateNbr;
    }

    public static class JobDashboardProgress implements JobContext.Metadata {

        private Long totalAmount;
        private Long succeededAmount;
        private Long failedAmount;
        private int progress;

        protected JobDashboardProgress() {
            // for json deserialization
        }

        public JobDashboardProgress(Long totalAmount) {
            if (totalAmount < 0L) throw new IllegalArgumentException("The total progress amount must be positive.");
            this.totalAmount = totalAmount;
            this.succeededAmount = 0L;
            this.failedAmount = 0L;
            if (totalAmount == 0) {
                progress = 100;
            }
        }

        public void incrementSucceeded() {
            setProgress(succeededAmount + 1);
        }

        public void incrementFailed() {
            setProgress(failedAmount + 1);
        }

        public boolean setProgress(Long succeededAmount) {
            return setProgress(this.totalAmount, succeededAmount, this.failedAmount);
        }

        public boolean setProgress(long totalAmount, long succeededAmount, long failedAmount) {
            boolean hasChanges = totalAmount < 1L || this.succeededAmount != succeededAmount || this.failedAmount != failedAmount || this.totalAmount != totalAmount;
            this.totalAmount = totalAmount;
            this.succeededAmount = succeededAmount;
            this.failedAmount = failedAmount;
            this.progress = (succeededAmount >= totalAmount) ? 100 : (int) (succeededAmount * 100 / totalAmount);
            return hasChanges;
        }

        public long getSucceededAmount() {
            return this.succeededAmount;
        }

        public Long getFailedAmount() {
            return failedAmount;
        }

        public int getProgress() {
            return progress;
        }

        public boolean hasSucceeded() {
            return progress == 100;
        }

        public Long getTotalAmount() {
            return totalAmount;
        }
    }
}
