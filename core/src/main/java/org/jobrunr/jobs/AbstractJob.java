package org.jobrunr.jobs;

import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.resilience.Lock;
import org.jobrunr.utils.resilience.Lockable;

import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;
import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;

public abstract class AbstractJob implements Lockable {

    private static final int MAX_AMOUNT_OF_LABELS = 3;
    private static final int MAX_LABEL_LENGTH = 45;
    private final transient Lock locker;

    private volatile int version;
    private String jobSignature;
    private String jobName;
    private Integer amountOfRetries;
    private HashSet<String> labels;
    private JobDetails jobDetails;

    protected AbstractJob() {
        // used for deserialization
        this.locker = new Lock();
        this.labels = new HashSet<>();
    }

    protected AbstractJob(JobDetails jobDetails) {
        this(jobDetails, 0);
    }

    protected AbstractJob(JobDetails jobDetails, int version) {
        this();
        this.jobDetails = jobDetails;
        this.version = version;
        this.jobSignature = JobUtils.getJobSignature(jobDetails);
    }

    public abstract Object getId();

    public int getVersion() {
        return version;
    }

    /**
     * Increases the version of this Job instance
     *
     * @return the version after it was increased
     */
    synchronized int increaseVersion() {
        return ++version;
    }

    void setVersion(int version) {
        this.version = version;
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

    public Integer getAmountOfRetries() {
        return amountOfRetries;
    }

    public void setAmountOfRetries(Integer retries) {
        this.amountOfRetries = retries;
    }

    public Set<String> getLabels() {
        return unmodifiableSet(labels);
    }

    public void setLabels(Set<String> labels) {
        if (isNotNullOrEmpty(labels)) {
            if (labels.size() > MAX_AMOUNT_OF_LABELS) {
                throw new IllegalArgumentException(String.format("Per job a maximum of %d labels can be provided.", MAX_AMOUNT_OF_LABELS));
            }
            if (labels.stream().anyMatch(label -> label.length() > 45)) {
                throw new IllegalArgumentException(String.format("Label length must be less than %d characters.", MAX_LABEL_LENGTH));
            }
            this.labels = new HashSet<>(labels);
        }
    }

    public JobDetails getJobDetails() {
        return jobDetails;
    }

    @Override
    public Lock lock() {
        return locker.lock();
    }

}
