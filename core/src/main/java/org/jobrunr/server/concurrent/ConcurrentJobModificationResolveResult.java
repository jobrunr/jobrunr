package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;

import java.io.Serializable;

public class ConcurrentJobModificationResolveResult implements Serializable {

    private final boolean succeeded;
    private final Job localJob;
    private Job jobFromStorage;

    private ConcurrentJobModificationResolveResult(boolean succeeded, Job localJob) {
        this.succeeded = succeeded;
        this.localJob = localJob;
    }

    private ConcurrentJobModificationResolveResult(boolean succeeded, Job localJob, Job jobFromStorage) {
        this.succeeded = succeeded;
        this.localJob = localJob;
        this.jobFromStorage = jobFromStorage;
    }

    public static ConcurrentJobModificationResolveResult succeeded(Job job) {
        return new ConcurrentJobModificationResolveResult(true, job);
    }

    public static ConcurrentJobModificationResolveResult failed(Job localJob, Job jobFromStorage) {
        return new ConcurrentJobModificationResolveResult(false, localJob, jobFromStorage);
    }

    public boolean failed() {
        return !succeeded;
    }

    public Job getLocalJob() {
        return localJob;
    }

    public Job getJobFromStorage() {
        return jobFromStorage;
    }
}

