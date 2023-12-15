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

    /**
     * Returns true of the current {@link AllowedConcurrentStateChange} can resolve the concurrent modified jobs.
     *
     * @param localState           the local state to match or null to match all local states
     * @param storageProviderState the storage provider state to match. May not be null.
     * @return true if this {@link AllowedConcurrentStateChange} can resolve the concurrent state change, false otherwise.
     */
    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        return (this.localState == null || this.localState == localState) && this.storageProviderState == storageProviderState;
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        //nothing more we can do
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }

}
