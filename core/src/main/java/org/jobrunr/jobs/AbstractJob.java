package org.jobrunr.jobs;

import org.jobrunr.utils.JobUtils;

public abstract class AbstractJob {

    private int version;
    private String jobSignature;
    private String jobName;
    private JobDetails jobDetails;

    protected AbstractJob() {
        // used for deserialization
    }

    public AbstractJob(JobDetails jobDetails) {
        this.jobSignature = JobUtils.getJobSignature(jobDetails);
        this.jobDetails = jobDetails;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Increases the version of this Job instance
     *
     * @return the version before it was increased
     */
    public int increaseVersion() {
        return version++;
    }

    public String getJobSignature() {
        return jobSignature;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public JobDetails getJobDetails() {
        return jobDetails;
    }
}
