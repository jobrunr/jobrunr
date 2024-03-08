package org.jobrunr.server.configuration;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;

public interface ConcurrentJobModificationPolicy {

    ConcurrentJobModificationResolver toConcurrentJobModificationResolver(BackgroundJobServer backgroundJobServer);
}
