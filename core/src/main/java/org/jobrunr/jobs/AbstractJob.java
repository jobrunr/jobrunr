package org.jobrunr.jobs;

import org.jobrunr.utils.JobUtils;

public abstract class AbstractJob {

    //private transient final Lock lock;
    private int version;
    private String jobSignature;
    private String jobName;
    private JobDetails jobDetails;

    protected AbstractJob() {
        // used for deserialization
        //this.lock = new Lock();
    }

    public AbstractJob(JobDetails jobDetails) {
        this();
        this.jobSignature = JobUtils.getJobSignature(jobDetails);
        this.jobDetails = jobDetails;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Increases the version of this Job instance
     * <p>
     * This must be thread safe.
     *
     * @return the version before it was increased
     */
    public synchronized int increaseVersion() {
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

//    @Override
//    public Lock lock() {
//        return lock.lock();
//    }
}
