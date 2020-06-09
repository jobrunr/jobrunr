package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

public interface AllowedConcurrentStateChange {

    boolean matches(StateName localState, StateName storageProviderState);

    ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob);
}
