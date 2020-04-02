package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.sse.ServerSentEventHandler;
import org.jobrunr.storage.JobStats;
import org.jobrunr.storage.JobStorageChangeListener;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.Timer;
import java.util.TimerTask;

public class JobRunrSseHandler extends ServerSentEventHandler implements JobStorageChangeListener {

    private final JsonMapper jsonMapper;

    public JobRunrSseHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this("/sse", storageProvider, jsonMapper);
    }

    public JobRunrSseHandler(String contextPath, StorageProvider storageProvider, JsonMapper jsonMapper) {
        super(contextPath);
        this.jsonMapper = jsonMapper;

        storageProvider.addJobStorageOnChangeListener(this);

        Timer timer = new Timer(true);
        timer.schedule(new SendJobStatsUpdate(this, storageProvider), 0, 5000);
    }

    public void emitObject(Object object) {
        if (jsonMapper == null) throw new IllegalStateException("You are trying to serialize an object but have not set a JsonMapper");
        emitMessage(jsonMapper.serialize(object));
    }

    @Override
    public void onChange(JobStats jobStats) {
        emitObject(jobStats);
    }

    static class SendJobStatsUpdate extends TimerTask {
        private final JobRunrSseHandler sseServer;
        private final StorageProvider storageProvider;

        public SendJobStatsUpdate(JobRunrSseHandler sseServer, StorageProvider storageProvider) {
            this.sseServer = sseServer;
            this.storageProvider = storageProvider;
        }

        public void run() {
            if (sseServer.hasNoSubscribers()) return;

            final JobStats jobStats = storageProvider.getJobStats();
            sseServer.emitObject(jobStats);
        }
    }
}
