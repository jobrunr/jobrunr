package org.jobrunr.stubs;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;

public class BackgroundJobServerStub extends BackgroundJobServer {

    public BackgroundJobServerStub(StorageProvider storageProvider, BackgroundJobServerStatus backgroundJobServerStatus) {
        super(storageProvider, null, backgroundJobServerStatus);

    }
}
