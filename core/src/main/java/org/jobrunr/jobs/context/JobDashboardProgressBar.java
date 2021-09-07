package org.jobrunr.jobs.context;

import org.jobrunr.jobs.Job;

import java.util.Map;

import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class JobDashboardProgressBar {

    public static final String JOBRUNR_PROGRESSBAR_KEY = "jobRunrDashboardProgressBar";

    private final JobDashboardProgress jobDashboardProgress;

    public JobDashboardProgressBar(Job job, Long totalAmount) {
        this.jobDashboardProgress = initJobDashboardProgress(job, totalAmount);
    }

    private JobDashboardProgress initJobDashboardProgress(Job job, Long totalAmount) {
        Map<String, Object> jobMetadata = job.getMetadata();
        String progressBarKey = progressBarKey(job.getJobStates().size());
        jobMetadata.putIfAbsent(progressBarKey, new JobDashboardProgress(totalAmount));
        return cast(jobMetadata.get(progressBarKey));
    }

    public void increaseByOne() {
        jobDashboardProgress.increaseByOne();
    }

    public int getProgress() {
        return jobDashboardProgress.getProgress();
    }

    public void setValue(int currentProgress) {
        this.setValue((long) currentProgress);
    }

    public void setValue(long currentProgress) {
        this.jobDashboardProgress.setCurrentValue(currentProgress);
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

    private static class JobDashboardProgress implements JobContext.Metadata {

        private Long totalAmount;
        private Long currentValue;
        private int progress;

        protected JobDashboardProgress() {
            // for json deserialization
        }

        public JobDashboardProgress(Long totalAmount) {
            if (totalAmount < 1) throw new IllegalArgumentException("The total progress amount must be larger than 0.");
            this.totalAmount = totalAmount;
            this.currentValue = 0L;
        }

        public void increaseByOne() {
            setCurrentValue(currentValue + 1);
        }

        public void setCurrentValue(Long currentValue) {
            this.currentValue = currentValue;
            this.progress = (int) (currentValue * 100 / totalAmount);
        }

        public int getProgress() {
            return progress;
        }
    }
}
