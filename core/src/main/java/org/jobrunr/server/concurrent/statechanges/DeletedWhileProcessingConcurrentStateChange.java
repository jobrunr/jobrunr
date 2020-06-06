package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;

public class DeletedWhileProcessingConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    public DeletedWhileProcessingConcurrentStateChange() {
        super(PROCESSING, DELETED);
    }

    @Override
    public void resolve(Job localJob, Job storageProviderJob) {

    }

}
