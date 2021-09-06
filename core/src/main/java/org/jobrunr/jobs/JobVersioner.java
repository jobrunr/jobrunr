package org.jobrunr.jobs;

public class JobVersioner implements AutoCloseable {

    private final Job job;
    private final int initialJobVersion;
    private boolean isVersionCommitted = false;


    public JobVersioner(Job job) {
        this.job = job;
        this.initialJobVersion = job.getVersion();
        this.job.increaseVersion();
    }

    public boolean isNewJob() {
        return this.initialJobVersion == 0;
    }

    public void commitVersion() {
        isVersionCommitted = true;
    }

    Job getJob() {
        return job;
    }

    @Override
    public void close() {
        if(!isVersionCommitted) {
            job.setVersion(initialJobVersion);
        }
    }
}
