package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.TeenyWebServer;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;

public class JobRunrDashboardWebServer {

    private final TeenyWebServer teenyWebServer;

    public static void main(String[] args) {
        new JobRunrDashboardWebServer(null, new JacksonJsonMapper());
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, 8000);
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, int port) {
        JobRunrStaticFileHandler staticFileHandler = new JobRunrStaticFileHandler();
        JobRunrApiHandler dashboardHandler = new JobRunrApiHandler(storageProvider, jsonMapper);
        JobRunrSseHandler sseHandler = new JobRunrSseHandler(storageProvider, jsonMapper);

        teenyWebServer = new TeenyWebServer(port);
        teenyWebServer.createContext(staticFileHandler);
        teenyWebServer.createContext(dashboardHandler);
        teenyWebServer.createContext(sseHandler);
        teenyWebServer.start();

    }

    public void stop() {
        teenyWebServer.stop();
    }



}
