package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class SucceededWhileAnyOtherConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    private final JobSteward jobSteward;

    public SucceededWhileAnyOtherConcurrentStateChange(JobSteward jobSteward) {
        super(null, StateName.SUCCEEDED);
        this.jobSteward = jobSteward;
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        // This means the job has been processed twice which we don't want
        if (localState == StateName.SUCCEEDED && storageProviderState == StateName.SUCCEEDED) return false;
        return super.matches(localState, storageProviderState);
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        if (localJob.getState() == StateName.SUCCEEDED && storageProviderJob.getState() == StateName.SUCCEEDED) {
            throw shouldNotHappenException("Should not happen as matches filter should be filtering out this StateChangeFilter");
        } else if (localJob.getState() == StateName.PROCESSING && storageProviderJob.getState() == StateName.SUCCEEDED) {
            localJob.delete("Job has already succeeded in StorageProvider");
            final Thread threadProcessingJob = jobSteward.getThreadProcessingJob(localJob);
            if (threadProcessingJob != null) {
                threadProcessingJob.interrupt();
            }
        }
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }
}
