package org.jobrunr.stubs;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.ServerZooKeeper;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public class BackgroundJobServerStub extends BackgroundJobServer {

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        super(storageProvider, jsonMapper, null, backgroundJobServerConfiguration);
    }

    @Override
    public BackgroundJobServerStatus getServerStatus() {
        final BackgroundJobServerStatus serverStatus = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(super.getServerStatus());
        serverStatus.start();
        return serverStatus;
    }
}
