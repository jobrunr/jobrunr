package org.jobrunr.storage.listeners;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobId;

public interface JobChangeListener extends StorageProviderChangeListener, AutoCloseable {

    JobId getJobId();

    void onChange(Job job);
}
