package org.jobrunr.jobs.filters;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.JobState;

import static java.time.Instant.now;
import static org.jobrunr.jobs.states.StateName.FAILED_STATES;

public class RetryFilter implements ElectStateFilter {

    private int numberOfRetries;

    public RetryFilter() {
        this(10);
    }

    public RetryFilter(int numberOfRetries) {
        this.numberOfRetries = numberOfRetries;
    }

    @Override
    public void onStateElection(Job job, JobState newState) {
        if (isNotFailed(newState) || isProblematicExceptionAndMustNotRetry(newState) || maxAmountOfRetriesReached(job)) return;

        job.scheduleAt(now().plusSeconds(getSecondsToAdd(job)), String.format("Retry %d of %d", getFailureCount(job), numberOfRetries));
    }

    protected long getSecondsToAdd(Job job) {
        return getExponentialBackoffPolicy(job, 3);
    }

    protected long getExponentialBackoffPolicy(Job job, int seed) {
        return (long) Math.pow(seed, getFailureCount(job));
    }

    private boolean maxAmountOfRetriesReached(Job job) {
        return getFailureCount(job) >= numberOfRetries;
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

}
