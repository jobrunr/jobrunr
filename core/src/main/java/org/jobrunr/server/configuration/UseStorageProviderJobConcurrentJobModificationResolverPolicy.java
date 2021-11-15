package org.jobrunr.server.configuration;

import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.UseStorageProviderJobConcurrentJobModificationResolver;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.annotations.Beta;

/**
 * A new policy of {@link ConcurrentJobModificationPolicy} that will always use the latest version of the job found in the database (SQL or NoSQL) in case of
 * a concurrent job modification exception.
 * <p>
 * Only use this {@link ConcurrentJobModificationPolicy} if you are altering jobs after they started processing (e.g. e.g. deleting, rescheduling, ...).
 * Using this {@link ConcurrentJobModificationPolicy} can result in the same job being executed multiple times.
 */
@Beta
public class UseStorageProviderJobConcurrentJobModificationResolverPolicy implements ConcurrentJobModificationPolicy {

    @Override
    public ConcurrentJobModificationResolver toConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        return new UseStorageProviderJobConcurrentJobModificationResolver(jobZooKeeper);
    }

}
