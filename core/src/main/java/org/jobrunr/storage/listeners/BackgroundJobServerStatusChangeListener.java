package org.jobrunr.storage.listeners;

import org.jobrunr.storage.BackgroundJobServerStatus;

import java.util.List;

public interface BackgroundJobServerStatusChangeListener extends StorageProviderChangeListener {

    void onChange(List<BackgroundJobServerStatus> changedServerStates);

}
