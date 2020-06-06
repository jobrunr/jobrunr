package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;

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

    public abstract void resolve(Job localJob, Job storageProviderJob);

}
