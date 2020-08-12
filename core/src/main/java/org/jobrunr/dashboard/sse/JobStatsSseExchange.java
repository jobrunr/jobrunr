package org.jobrunr.dashboard.sse;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;

public class JobStatsSseExchange extends AbstractObjectSseExchange implements JobStatsChangeListener {

    private static String lastMessage;

    private final StorageProvider storageProvider;

    public JobStatsSseExchange(HttpExchange httpExchange, StorageProvider storageProvider, JsonMapper jsonMapper) throws IOException {
        super(httpExchange, jsonMapper);
        this.storageProvider = storageProvider;
        storageProvider.addJobStorageOnChangeListener(this);
        sendMessage(lastMessage);
    }

    @Override
    public void onChange(JobStats jobStats) {
        lastMessage = sendObject(jobStats);
    }

    @Override
    public void close() {
        storageProvider.removeJobStorageOnChangeListener(this);
        super.close();
    }
}
