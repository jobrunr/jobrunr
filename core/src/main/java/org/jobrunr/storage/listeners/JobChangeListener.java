package org.jobrunr.storage.listeners;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobId;

public interface JobChangeListener extends JobStorageChangeListener, AutoCloseable {

    JobId getJobId();

    void onChange(Job job);
}
