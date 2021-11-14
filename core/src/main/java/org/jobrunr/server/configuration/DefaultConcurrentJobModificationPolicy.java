package org.jobrunr.server.configuration;

import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.DefaultConcurrentJobModificationResolver;
import org.jobrunr.storage.StorageProvider;

public class DefaultConcurrentJobModificationPolicy implements ConcurrentJobModificationPolicy {

    @Override
    public ConcurrentJobModificationResolver toConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper) {
        return new DefaultConcurrentJobModificationResolver(storageProvider, jobZooKeeper);
    }

}
