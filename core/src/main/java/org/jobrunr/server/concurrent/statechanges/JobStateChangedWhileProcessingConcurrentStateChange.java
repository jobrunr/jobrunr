package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.jobrunr.utils.annotations.Because;

import static org.jobrunr.jobs.states.StateName.PROCESSING;

// in some rare cases (due to GC or no CPU allocation), a job might fail or succeed (by the JobPerformer) and will also be saved to the database
// by the jobZooKeeper.updateJobsThatAreBeingProcessed().
// If the job has succeeded or failed and is not being processed anymore (so no Thread processing it in the JobZooKeeper), we can discard the local job.
@Because("https://github.com/jobrunr/jobrunr/issues/631")
public class JobStateChangedWhileProcessingConcurrentStateChange implements AllowedConcurrentStateChange {

    private final JobZooKeeper jobZooKeeper;

    public JobStateChangedWhileProcessingConcurrentStateChange(JobZooKeeper jobZooKeeper) {
        this.jobZooKeeper = jobZooKeeper;
    }

    @Override
    public boolean matches(Job localJob, Job storageProviderJob) {
        if(storageProviderJob.getVersion() == localJob.getVersion() + 1
                && localJob.hasState(PROCESSING) && !storageProviderJob.hasState(PROCESSING)) {
            return jobZooKeeper.getThreadProcessingJob(localJob) == null;
        }
        return false;
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        throw new IllegalStateException("Should not happen as matches method is overridden");
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        return ConcurrentJobModificationResolveResult.succeeded(storageProviderJob);
    }
}
