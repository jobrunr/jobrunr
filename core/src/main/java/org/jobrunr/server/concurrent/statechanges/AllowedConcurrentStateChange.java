package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;

public interface AllowedConcurrentStateChange {

    boolean matches(StateName localState, StateName storageProviderState);

    void resolve(Job localJob, Job storageProviderJob);
}
