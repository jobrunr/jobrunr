package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.sse.ServerSentEventHandler;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.JobStorageChangeListener;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

public class JobRunrSseHandler extends ServerSentEventHandler implements JobStorageChangeListener {

    private final StorageProvider storageProvider;
    private final JsonMapper jsonMapper;

    public JobRunrSseHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this("/sse", storageProvider, jsonMapper);
    }

    public JobRunrSseHandler(String contextPath, StorageProvider storageProvider, JsonMapper jsonMapper) {
        super(contextPath);
        this.storageProvider = storageProvider;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void subscribersChanged(int amount) {
        if (amount > 0) {
            storageProvider.addJobStorageOnChangeListener(this);
        } else {
            storageProvider.removeJobStorageOnChangeListener(this);
        }
    }

    @Override
    public void onChange(JobStats jobStats) {
        emitObject(jobStats);
    }

    public void emitObject(Object object) {
        if (jsonMapper == null) throw new IllegalStateException("You are trying to serialize an object but have not set a JsonMapper");
        emitMessage(jsonMapper.serialize(object));
    }
}
