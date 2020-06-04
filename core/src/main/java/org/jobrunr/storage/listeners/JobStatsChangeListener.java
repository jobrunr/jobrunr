package org.jobrunr.storage.listeners;

import org.jobrunr.storage.JobStats;

public interface JobStatsChangeListener extends JobStorageChangeListener {

    void onChange(JobStats jobStats);

}
