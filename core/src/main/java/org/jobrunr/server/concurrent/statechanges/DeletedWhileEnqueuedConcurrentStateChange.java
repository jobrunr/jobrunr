package org.jobrunr.server.concurrent.statechanges;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;

/**
 * Needed for jobs that go from scheduled state to enqueued state while the job was already deleted.
 * See Github issue https://github.com/jobrunr/jobrunr/issues/210
 */
public class DeletedWhileEnqueuedConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    public DeletedWhileEnqueuedConcurrentStateChange() {
        super(ENQUEUED, DELETED);
    }

}
