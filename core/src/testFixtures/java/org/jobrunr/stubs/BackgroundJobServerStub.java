package org.jobrunr.stubs;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.carbonaware.CarbonAwareJobManager;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

public class BackgroundJobServerStub extends BackgroundJobServer {

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        super(storageProvider, null, jsonMapper, null, backgroundJobServerConfiguration);
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        super(storageProvider, null, jsonMapper, null, backgroundJobServerConfiguration);
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration, CarbonAwareJobManager carbonAwareJobManager) {
        super(storageProvider, carbonAwareJobManager, jsonMapper, null, backgroundJobServerConfiguration);
    }

    @Override
    public BackgroundJobServerStatus getServerStatus() {
        return aDefaultBackgroundJobServerStatus().withIsStarted().build();
    }
}
