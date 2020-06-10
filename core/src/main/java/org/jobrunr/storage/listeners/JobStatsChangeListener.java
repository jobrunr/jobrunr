package org.jobrunr.storage.listeners;

import org.jobrunr.storage.JobStats;

public interface JobStatsChangeListener extends StorageProviderChangeListener {

    void onChange(JobStats jobStats);

}
