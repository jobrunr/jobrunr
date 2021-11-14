package org.jobrunr.server.configuration;

import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.storage.StorageProvider;

public class UseStorageProviderJobConcurrentJobModificationResolver implements ConcurrentJobModificationPolicy {

    @Override
    public ConcurrentJobModificationResolver toConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        return new org.jobrunr.server.concurrent.UseStorageProviderJobConcurrentJobModificationResolver(jobZooKeeper);
    }

}
