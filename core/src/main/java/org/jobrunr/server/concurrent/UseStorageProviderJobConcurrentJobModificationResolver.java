package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.IllegalJobStateChangeException;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.utils.annotations.Beta;

import java.util.List;

/**
 * A new implementation of {@link ConcurrentJobModificationResolver} that will always use the latest version of the job found in the database (SQL or NoSQL).
 * <p>
 * Only use this {@link ConcurrentJobModificationResolver} if you are altering jobs after they started processing (e.g. e.g. deleting, rescheduling, ...).
 * Using this {@link ConcurrentJobModificationResolver} can result in the same job being executed multiple times.
 */
@Beta
public class UseStorageProviderJobConcurrentJobModificationResolver implements ConcurrentJobModificationResolver {

    private final JobZooKeeper jobZooKeeper;

    public UseStorageProviderJobConcurrentJobModificationResolver(JobZooKeeper jobZooKeeper) {
        this.jobZooKeeper = jobZooKeeper;
    }

    @Override
    public void resolve(ConcurrentJobModificationException e) {
        final List<Job> concurrentUpdatedJobs = e.getConcurrentUpdatedJobs();
        concurrentUpdatedJobs
                .forEach(job -> resolve(job, e));
    }

    public ConcurrentJobModificationResolveResult resolve(final Job localJob, ConcurrentJobModificationException e) {
        failLocalIfPossible(localJob, e);
        final Thread threadProcessingJob = jobZooKeeper.getThreadProcessingJob(localJob);
        if (threadProcessingJob != null) {
            threadProcessingJob.interrupt();
        }
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }

    private void failLocalIfPossible(Job localJob, ConcurrentJobModificationException e) {
        try {
            localJob.failed("Job is already updated in StorageProvider, discarding local job.", e);
        } catch (IllegalJobStateChangeException ignored) {
            // we don't care as we will only continue with the job from the storage provider.
        }
    }

}
