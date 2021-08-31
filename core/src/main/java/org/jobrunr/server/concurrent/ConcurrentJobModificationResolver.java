package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.statechanges.*;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ConcurrentJobModificationResolver {

    private final StorageProvider storageProvider;
    private final List<AllowedConcurrentStateChange> allowedConcurrentStateChanges;

    public ConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        this.storageProvider = storageProvider;
        allowedConcurrentStateChanges = Arrays.asList(
                new DeletedWhileProcessingConcurrentStateChange(jobZooKeeper),
                new DeletedWhileSucceededConcurrentStateChange(),
                new DeletedWhileFailedConcurrentStateChange(),
                new DeletedWhileEnqueuedConcurrentStateChange(),
                new DeletedWhileScheduledConcurrentStateChange()
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
                .filter(allowedConcurrentStateChange -> allowedConcurrentStateChange.matches(localJob.getState(), jobFromStorage.getState()))
                .findFirst()
                .map(allowedConcurrentStateChange -> allowedConcurrentStateChange.resolve(localJob, jobFromStorage))
                .orElse(ConcurrentJobModificationResolveResult.failed(localJob, jobFromStorage));
    }

    private Job getJobFromStorageProvider(Job localJob) {
        return storageProvider.getJobById(localJob.getId());
    }

}
