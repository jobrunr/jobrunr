package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

public interface AllowedConcurrentStateChange {

    default boolean matches(Job localJob, Job storageProviderJob) {
        return matches(localJob.getState(), storageProviderJob.getState());
    }

    boolean matches(StateName localState, StateName storageProviderState);

    ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob);
}
