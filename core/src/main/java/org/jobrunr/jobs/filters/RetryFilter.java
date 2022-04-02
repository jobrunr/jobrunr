package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.JobUtils;

import static java.time.Instant.now;
import static org.jobrunr.jobs.states.StateName.FAILED_STATES;

/**
 * A JobFilter of type {@link ElectStateFilter} that will retry the job if it fails for up to 10 times with an exponential back-off policy.
 * This JobFilter is added by default in JobRunr.
 * <p>
 * If you want to configure the amount of retries, create a new instance and pass it to the JobRunrConfiguration, e.g.:
 *
 * <pre>
 *     JobRunr.configure()
 *                 ...
 *                 .withJobFilter(new RetryFilter(20, 4)) // this will result in 20 retries and the retries will happen after 4 seconds, 16 seconds, 64 seconds, ...
 *                 ...
 *                 .initialize();
 * </pre>
 */
public class RetryFilter implements ElectStateFilter {

    public static final int DEFAULT_BACKOFF_POLICY_TIME_SEED = 3;
    public static final int DEFAULT_NBR_OF_RETRIES = 10;

    private final int numberOfRetries;
    private final int backOffPolicyTimeSeed;

    public RetryFilter() {
        this(DEFAULT_NBR_OF_RETRIES);
    }

    public RetryFilter(int numberOfRetries) {
        this(numberOfRetries, DEFAULT_BACKOFF_POLICY_TIME_SEED);
    }

    public RetryFilter(int numberOfRetries, int backOffPolicyTimeSeed) {
        this.numberOfRetries = numberOfRetries;
        this.backOffPolicyTimeSeed = backOffPolicyTimeSeed;
    }

    @Override
    public void onStateElection(Job job, JobState newState) {
        if (isNotFailed(newState) || isProblematicExceptionAndMustNotRetry(newState) || maxAmountOfRetriesReached(job))
            return;

        job.scheduleAt(now().plusSeconds(getSecondsToAdd(job)), String.format("Retry %d of %d", getFailureCount(job), getMaxNumberOfRetries(job)));
    }

    protected long getSecondsToAdd(Job job) {
        return getExponentialBackoffPolicy(job, backOffPolicyTimeSeed);
    }

    protected long getExponentialBackoffPolicy(Job job, int seed) {
        return (long) Math.pow(seed, getFailureCount(job));
    }

    private boolean maxAmountOfRetriesReached(Job job) {
        return getFailureCount(job) > getMaxNumberOfRetries(job);
    }

    private long getFailureCount(Job job) {
        return job.getJobStates().stream().filter(FAILED_STATES).count();
    }

    private boolean isProblematicExceptionAndMustNotRetry(JobState newState) {
        if (newState instanceof FailedState) {
            return ((FailedState) newState).mustNotRetry();
        }
        return false;
    }

    private boolean isNotFailed(JobState newState) {
        return !(newState instanceof FailedState);
    }

    private int getMaxNumberOfRetries(Job job) {
        return JobUtils.getJobAnnotation(job.getJobDetails())
                .map(jobAnnotation -> jobAnnotation.retries() > org.jobrunr.jobs.annotations.Job.NBR_OF_RETRIES_NOT_PROVIDED ? jobAnnotation.retries() : null)
                .orElse(this.numberOfRetries);
    }
}
