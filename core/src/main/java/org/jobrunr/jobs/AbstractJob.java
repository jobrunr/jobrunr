package org.jobrunr.jobs;

import org.jobrunr.utils.JobUtils;
import org.jobrunr.utils.resilience.Lock;
import org.jobrunr.utils.resilience.Lockable;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toCollection;
import static org.jobrunr.utils.CollectionUtils.isNotNullOrEmpty;

public abstract class AbstractJob implements Lockable {

    private static final int MAX_AMOUNT_OF_LABELS = 3;
    private static final int MAX_LABEL_LENGTH = 45;
    private final transient Lock locker;

    private volatile int version;
    private String jobSignature;
    private String jobName;
    private Integer amountOfRetries;
    private ArrayList<String> labels;
    private JobDetails jobDetails;

    protected AbstractJob() {
        // used for deserialization
        this.locker = new Lock();
        this.labels = new ArrayList<>();
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

    public List<String> getLabels() {
        return unmodifiableList(labels);
    }

    public void setLabels(List<String> labels) {
        if (isNotNullOrEmpty(labels)) {
            if (labels.size() > MAX_AMOUNT_OF_LABELS) {
                throw new IllegalArgumentException(String.format("Per job a maximum of %d labels can be provided.", MAX_AMOUNT_OF_LABELS));
            }
            if (labels.stream().anyMatch(label -> label.length() > 45)) {
                throw new IllegalArgumentException(String.format("Label length must be less than %d characters.", MAX_LABEL_LENGTH));
            }
            this.labels = labels.stream().distinct().collect(toCollection(ArrayList::new));
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
