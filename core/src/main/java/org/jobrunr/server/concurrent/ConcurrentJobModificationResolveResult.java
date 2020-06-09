package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;

public class ConcurrentJobModificationResolveResult {

    private final boolean succeeded;
    private final Job job;

    private ConcurrentJobModificationResolveResult(boolean succeeded, Job job) {
        this.succeeded = succeeded;
        this.job = job;
    }

    public static ConcurrentJobModificationResolveResult succeeded(Job job) {
        return new ConcurrentJobModificationResolveResult(true, job);
    }

    public static ConcurrentJobModificationResolveResult failed(Job job) {
        return new ConcurrentJobModificationResolveResult(false, job);
    }

    public boolean failed() {
        return !succeeded;
    }

    public Job getJob() {
        return job;
    }
}
