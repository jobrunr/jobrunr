package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.TeenyWebServer;
import org.jobrunr.dashboard.server.http.RedirectHttpHandler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public class JobRunrDashboardWebServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobRunrDashboardWebServer.class);

    private final TeenyWebServer teenyWebServer;

    public static void main(String[] args) {
        new JobRunrDashboardWebServer(null, new JacksonJsonMapper());
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, 8000);
    }

    public JobRunrDashboardWebServer(StorageProvider storageProvider, JsonMapper jsonMapper, int port) {
        RedirectHttpHandler redirectHttpHandler = new RedirectHttpHandler("/", "/dashboard");
        JobRunrStaticFileHandler staticFileHandler = new JobRunrStaticFileHandler();
        JobRunrApiHandler dashboardHandler = new JobRunrApiHandler(storageProvider, jsonMapper);
        JobRunrSseHandler sseHandler = new JobRunrSseHandler(storageProvider, jsonMapper);

        teenyWebServer = new TeenyWebServer(port);
        teenyWebServer.createContext(redirectHttpHandler);
        teenyWebServer.createContext(staticFileHandler);
        teenyWebServer.createContext(dashboardHandler);
        teenyWebServer.createContext(sseHandler);
        teenyWebServer.start();

        LOGGER.info(format("JobRunr dashboard started at http://%s:%d%s",
                teenyWebServer.getWebServerHostAddress(),
                teenyWebServer.getWebServerHostPort(),
                staticFileHandler.getContextPath()));

    }

    public void stop() {
        teenyWebServer.stop();
    }

}
