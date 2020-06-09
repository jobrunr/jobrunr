package org.jobrunr.server.concurrent.statechanges;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.FAILED;

public class DeletedWhileFailedConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    public DeletedWhileFailedConcurrentStateChange() {
        super(FAILED, DELETED);
    }

}
