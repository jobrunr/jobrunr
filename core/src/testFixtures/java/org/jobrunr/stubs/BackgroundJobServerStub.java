package org.jobrunr.stubs;

import org.jobrunr.jobs.JobConfiguration;
import org.jobrunr.jobs.JobConfigurationReader;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.BackgroundJobServerConfigurationReader;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import static org.jobrunr.jobs.JobConfiguration.usingStandardJobConfiguration;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

public class BackgroundJobServerStub extends BackgroundJobServer {

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, usingStandardJobConfiguration(), usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, JobConfiguration jobConfiguration, BackgroundJobServerConfiguration backgroundJobServerConfiguration) {
        super(storageProvider, jsonMapper, null, jobConfiguration, backgroundJobServerConfiguration);
    }

    public BackgroundJobServerStub(StorageProvider storageProvider, JsonMapper jsonMapper, JobConfigurationReader jobConfigurationReader, BackgroundJobServerConfigurationReader backgroundJobServerConfiguration) {
        super(storageProvider, jsonMapper, null, jobConfigurationReader, backgroundJobServerConfiguration);
    }

    @Override
    public BackgroundJobServerStatus getServerStatus() {
        return aDefaultBackgroundJobServerStatus().withIsStarted().build();
    }
}
