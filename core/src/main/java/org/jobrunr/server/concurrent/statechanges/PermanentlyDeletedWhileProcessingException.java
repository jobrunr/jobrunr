package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;

public class PermanentlyDeletedWhileProcessingException extends AbstractAllowedConcurrentStateChange {

    private final JobZooKeeper jobZooKeeper;

    public PermanentlyDeletedWhileProcessingException(JobZooKeeper jobZooKeeper) {
        super(PROCESSING, DELETED);
        this.jobZooKeeper = jobZooKeeper;
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
        final Thread threadProcessingJob = jobZooKeeper.getThreadProcessingJob(localJob);
        if (threadProcessingJob != null) {
            threadProcessingJob.interrupt();
        }
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }
}
