package org.jobrunr.storage;

public interface JobStorageChangeListener {

    void onChange(JobStats jobStats);

}
