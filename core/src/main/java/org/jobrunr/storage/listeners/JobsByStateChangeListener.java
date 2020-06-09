package org.jobrunr.storage.listeners;

import org.jobrunr.storage.JobStats;

public interface JobsByStateChangeListener extends JobStorageChangeListener {

    void onChange(JobStats jobStats);

}
