package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class DeletedWhileAnyOtherConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    private final JobSteward jobSteward;

    public DeletedWhileAnyOtherConcurrentStateChange(JobSteward jobSteward) {
        super(null, StateName.DELETED);
        this.jobSteward = jobSteward;
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        // This means the job does not listen to interrupts
        if (localState == StateName.DELETED && storageProviderState == StateName.DELETED) return false;
        return super.matches(localState, storageProviderState);
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        if (localJob.getState() == StateName.DELETED && storageProviderJob.getState() == StateName.DELETED) {
            throw shouldNotHappenException("Should not happen as matches filter should be filtering out this StateChangeFilter");
        } else if (localJob.getState() == StateName.PROCESSING && storageProviderJob.getState() == StateName.DELETED) {
            localJob.delete("Job is already deleted in StorageProvider");
            final Thread threadProcessingJob = jobSteward.getThreadProcessingJob(localJob);
            if (threadProcessingJob != null) {
                threadProcessingJob.interrupt();
            }
        }
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }
}
