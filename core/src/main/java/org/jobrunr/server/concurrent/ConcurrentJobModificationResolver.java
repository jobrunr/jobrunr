package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.concurrent.statechanges.AllowedConcurrentStateChange;
import org.jobrunr.server.concurrent.statechanges.DeletedWhileProcessingConcurrentStateChange;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ConcurrentJobModificationResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentJobModificationResolver.class);

    private static final List<AllowedConcurrentStateChange> ALLOWED_CONCURRENT_STATE_CHANGES = Arrays.asList(
            new DeletedWhileProcessingConcurrentStateChange()
    );

    private final StorageProvider storageProvider;

    public ConcurrentJobModificationResolver(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    public void resolve(final Job localJob, final ConcurrentJobModificationException exception) {
        final Job storageProviderJob = getJobFromStorageProvider(exception);
        final AllowedConcurrentStateChange concurrentStateChange = ALLOWED_CONCURRENT_STATE_CHANGES
                .stream()
                .filter(allowedConcurrentStateChange -> allowedConcurrentStateChange.matches(localJob.getState(), storageProviderJob.getState()))
                .findAny()
                .orElseThrow(() -> exception);
        concurrentStateChange.resolve(storageProviderJob, localJob);
    }

    private Job getJobFromStorageProvider(ConcurrentJobModificationException exception) {
        final UUID jobId = getJobId(exception);
        return storageProvider.getJobById(jobId);
    }

    private UUID getJobId(ConcurrentJobModificationException exception) {
        UUID jobId = exception.getJobId();
        if (jobId == null) {
            LOGGER.error("Cannot resolve ConcurrentJobModificationException as jobId is unknown");
            throw exception;
        }
        return jobId;
    }
}
