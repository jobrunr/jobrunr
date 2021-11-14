package org.jobrunr.server.configuration;

import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.storage.StorageProvider;

public interface ConcurrentJobModificationPolicy {

    ConcurrentJobModificationResolver toConcurrentJobModificationResolver(StorageProvider storageProvider, JobZooKeeper jobZooKeeper);
}
