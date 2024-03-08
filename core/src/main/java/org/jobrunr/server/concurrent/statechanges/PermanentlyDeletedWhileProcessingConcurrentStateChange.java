package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

public class PermanentlyDeletedWhileProcessingConcurrentStateChange extends AbstractAllowedConcurrentStateChange {

    private final JobSteward jobSteward;

    public PermanentlyDeletedWhileProcessingConcurrentStateChange(JobSteward jobSteward) {
        super(null, null);
        this.jobSteward = jobSteward;
    }

    @Override
    public boolean matches(Job localJob, Job storageProviderJob) {
        return storageProviderJob == null;
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        throw new IllegalStateException("Should not happen");
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        localJob.delete("Job is already deleted in StorageProvider");
        final Thread threadProcessingJob = jobSteward.getThreadProcessingJob(localJob);
        if (threadProcessingJob != null) {
            threadProcessingJob.interrupt();
        }
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }
}
