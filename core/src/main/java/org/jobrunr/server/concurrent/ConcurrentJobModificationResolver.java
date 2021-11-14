package org.jobrunr.server.concurrent;

import org.jobrunr.storage.ConcurrentJobModificationException;

/**
 * Class responsible for resolving ConcurrentJobModificationExceptions.
 * <p>
 * Concurrent Job Modifications can occur if:
 * - you are updating jobs (e.g. deleting or scheduling) that are already being processed by JobRunr
 * - there is a bug in the StorageProvider
 * <p>
 * See {@link DefaultConcurrentJobModificationResolver} and {@link UseStorageProviderJobConcurrentJobModificationResolver} for implementations.
 */
public interface ConcurrentJobModificationResolver {

    void resolve(ConcurrentJobModificationException e);

}
