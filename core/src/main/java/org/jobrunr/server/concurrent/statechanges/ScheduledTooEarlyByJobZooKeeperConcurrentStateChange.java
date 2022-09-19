package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobVersioner;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.annotations.Because;

// in some rare cases, a job might fail and be rescheduled and will be saved to the database by jobZooKeeper.updateJobsThatAreBeingProcessed() and then again
// by the JobPerformer. In this case, the master tries to save the newly enqueued job again but in the mean time the job version has been updated as the
// JobPerformer also saved it.
// This occurs mostly when a lot of jobs fail.
@Because({"https://github.com/jobrunr/jobrunr/issues/557", "https://github.com/jobrunr/jobrunr/issues/553"})
public class ScheduledTooEarlyByJobZooKeeperConcurrentStateChange implements AllowedConcurrentStateChange {

    private final StorageProvider storageProvider;

    public ScheduledTooEarlyByJobZooKeeperConcurrentStateChange(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    @Override
    public boolean matches(Job localJob, Job storageProviderJob) {
        return AllowedConcurrentStateChange.super.matches(localJob, storageProviderJob)
                && localJob.getVersion() == storageProviderJob.getVersion() - 1
                && localJob.getLastJobStateOfType(FailedState.class).isPresent();
    }

    @Override
    public boolean matches(StateName localState, StateName storageProviderState) {
        return localState == StateName.ENQUEUED && storageProviderState == StateName.SCHEDULED;
    }

    @Override
    public ConcurrentJobModificationResolveResult resolve(Job localJob, Job storageProviderJob) {
        //why: we use the JobVersioner to bump the version so it matched the one from the DB
        new JobVersioner(localJob);
        storageProvider.save(localJob);
        return ConcurrentJobModificationResolveResult.succeeded(localJob);
    }
}
