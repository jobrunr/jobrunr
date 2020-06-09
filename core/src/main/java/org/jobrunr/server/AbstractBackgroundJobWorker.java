package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.jobs.states.StateName;

public abstract class AbstractBackgroundJobWorker implements Runnable {

    protected final BackgroundJobServer backgroundJobServer;
    protected final Job job;
    protected final JobFilters jobFilters;

    public AbstractBackgroundJobWorker(BackgroundJobServer backgroundJobServer, Job job) {
        this.backgroundJobServer = backgroundJobServer;
        this.job = job;
        this.jobFilters = backgroundJobServer.getJobFilters();
    }

    protected void saveAndRunStateRelatedJobFilters(Job job) {
        jobFilters.runOnStateAppliedFilters(job);
        StateName beforeStateElection = job.getState();
        jobFilters.runOnStateElectionFilter(job);
        StateName afterStateElection = job.getState();
        this.backgroundJobServer.getStorageProvider().save(job);
        if (beforeStateElection != afterStateElection) {
            jobFilters.runOnStateAppliedFilters(job);
        }
    }
}
