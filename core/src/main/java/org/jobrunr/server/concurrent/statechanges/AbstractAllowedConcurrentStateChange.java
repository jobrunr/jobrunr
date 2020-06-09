package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

public abstract class AbstractAllowedConcurrentStateChange implements AllowedConcurrentStateChange {

    private final StateName localState;
    private final StateName storageProviderState;

    protected AbstractAllowedConcurrentStateChange(StateName localState, StateName storageProviderState) {
        this.localState = localState;
        this.storageProviderState = storageProviderState;
    }

    public boolean matches(StateName localState, StateName storageProviderState) {
        return this.localState == localState && this.storageProviderState == storageProviderState;
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        //nothing more we can do
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }

}
