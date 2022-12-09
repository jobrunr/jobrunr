package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.jobrunr.utils.annotations.Because;

import java.util.Optional;

// if a BackgroundJobServer becomes unresponsive (like due to garbage collection or other) and the job has been picked up by another BackgroundJobServer,
// this will be resolved.
@Because("https://github.com/jobrunr/jobrunr/issues/429")
public class JobPerformedOnOtherBackgroundJobServerConcurrentStateChange implements AllowedConcurrentStateChange {

    private JobZooKeeper jobZooKeeper;

    public JobPerformedOnOtherBackgroundJobServerConcurrentStateChange(JobZooKeeper jobZooKeeper) {
        this.jobZooKeeper = jobZooKeeper;
    }

    @Override
    public boolean matches(Job localJob, Job storageProviderJob) {
        if(storageProviderJob.getVersion() > localJob.getVersion() + 1) {
            Optional<ProcessingState> localProcessingState = localJob.getLastJobStateOfType(ProcessingState.class);
            Optional<ProcessingState> storageProviderProcessingState = storageProviderJob.getLastJobStateOfType(ProcessingState.class);
            if(localProcessingState.isPresent() && storageProviderProcessingState.isPresent()) {
                return !localProcessingState.get().getServerId().equals(storageProviderProcessingState.get().getServerId());
            }
        }
        return false;
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        throw new IllegalStateException("Should not happen as matches method is overridden");
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        final Thread threadProcessingJob = jobZooKeeper.getThreadProcessingJob(localJob);
        if (threadProcessingJob != null) {
            threadProcessingJob.interrupt();
        }
        return ConcurrentJobModificationResolveResult.succeeded(storageProviderJob);
    }
}
