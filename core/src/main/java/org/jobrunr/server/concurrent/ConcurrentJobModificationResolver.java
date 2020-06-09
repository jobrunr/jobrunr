package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.statechanges.AllowedConcurrentStateChange;
import org.jobrunr.server.concurrent.statechanges.DeletedWhileFailedConcurrentStateChange;
import org.jobrunr.server.concurrent.statechanges.DeletedWhileProcessingConcurrentStateChange;
import org.jobrunr.server.concurrent.statechanges.DeletedWhileScheduledConcurrentStateChange;
import org.jobrunr.server.concurrent.statechanges.DeletedWhileSucceededConcurrentStateChange;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class ConcurrentJobModificationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentJobModificationResolver.class);

    private final StorageProvider storageProvider;
    private final List<AllowedConcurrentStateChange> allowedConcurrentStateChanges;

    public ConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        this.storageProvider = storageProvider;
        allowedConcurrentStateChanges = Arrays.asList(
                new DeletedWhileProcessingConcurrentStateChange(jobZooKeeper),
                new DeletedWhileSucceededConcurrentStateChange(),
                new DeletedWhileFailedConcurrentStateChange(),
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
            throw new ConcurrentJobModificationException(failedToResolve.stream().map(ConcurrentJobModificationResolveResult::getJob).collect(toList()));
        }
    }

    public ConcurrentJobModificationResolveResult resolve(final Job localJob) {
        final Job storageProviderJob = getJobFromStorageProvider(localJob);
        return allowedConcurrentStateChanges
                .stream()
                .filter(allowedConcurrentStateChange -> allowedConcurrentStateChange.matches(localJob.getState(), storageProviderJob.getState()))
                .findFirst()
                .map(allowedConcurrentStateChange -> allowedConcurrentStateChange.resolve(localJob, storageProviderJob))
                .orElse(ConcurrentJobModificationResolveResult.failed(localJob));
    }

    private Job getJobFromStorageProvider(Job localJob) {
        return storageProvider.getJobById(localJob.getId());
    }

}
