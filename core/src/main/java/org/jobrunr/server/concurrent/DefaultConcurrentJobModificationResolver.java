package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.statechanges.*;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.StorageProvider;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Default implementation of {@link ConcurrentJobModificationResolver}.
 * <p>
 * If Jobs are deleted, the {@link DefaultConcurrentJobModificationResolver} will resolve the concurrent job modification
 * by stopping the processing of the job. For other concurrent modifications, the {@link DefaultConcurrentJobModificationResolver} will
 * throw {@link UnresolvableConcurrentJobModificationException} as these may point to programming errors (JobRunr was conceived with the idea that once a
 * job is being processed, it should not be modified anymore).
 */
public class DefaultConcurrentJobModificationResolver implements ConcurrentJobModificationResolver {

    private final StorageProvider storageProvider;
    private final List<AllowedConcurrentStateChange> allowedConcurrentStateChanges;

    public DefaultConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        this.storageProvider = storageProvider;
        allowedConcurrentStateChanges = Arrays.asList(
                new PermanentlyDeletedWhileProcessingConcurrentStateChange(jobZooKeeper),
                new DeletedWhileProcessingConcurrentStateChange(jobZooKeeper),
                new DeletedWhileSucceededConcurrentStateChange(),
                new DeletedWhileFailedConcurrentStateChange(),
                new DeletedWhileEnqueuedConcurrentStateChange(),
                new DeletedWhileScheduledConcurrentStateChange(),
                new JobStateChangedWhileProcessingConcurrentStateChange(jobZooKeeper),
                new JobPerformedOnOtherBackgroundJobServerConcurrentStateChange(jobZooKeeper),
                new ScheduledTooEarlyByJobZooKeeperConcurrentStateChange(storageProvider)
        );
    }

    public void resolve(ConcurrentJobModificationException e) {
        final List<Job> concurrentUpdatedJobs = e.getConcurrentUpdatedJobs();
        final List<ConcurrentJobModificationResolveResult> failedToResolve = concurrentUpdatedJobs
                .stream()
                .map(this::resolve)
                .filter(ConcurrentJobModificationResolveResult::failed)
                .collect(toList());

        if (!failedToResolve.isEmpty()) {
            throw new UnresolvableConcurrentJobModificationException(failedToResolve);
        }
    }

    public ConcurrentJobModificationResolveResult resolve(final Job localJob) {
        final Job jobFromStorage = getJobFromStorageProvider(localJob);
        return allowedConcurrentStateChanges
                .stream()
                .filter(allowedConcurrentStateChange -> allowedConcurrentStateChange.matches(localJob, jobFromStorage))
                .findFirst()
                .map(allowedConcurrentStateChange -> allowedConcurrentStateChange.resolve(localJob, jobFromStorage))
                .orElse(ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage));
    }


    private Job getJobFromStorageProvider(Job localJob) {
        try {
            return storageProvider.getJobById(localJob.getId());
        } catch (JobNotFoundException e) {
            // this happens when the job was permanently deleted while processing
            return null;
        }
    }

}
