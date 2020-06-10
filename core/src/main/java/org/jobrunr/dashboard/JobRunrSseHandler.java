package org.jobrunr.dashboard;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.dashboard.server.sse.ServerSentEventHandler;
import org.jobrunr.dashboard.server.sse.SseExchange;
import org.jobrunr.dashboard.sse.BackgroundJobServerStatusSseExchange;
import org.jobrunr.dashboard.sse.JobSseExchange;
import org.jobrunr.dashboard.sse.JobStatsSseExchange;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;

public class JobRunrSseHandler extends ServerSentEventHandler {

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
    protected SseExchange createSseExchange(HttpExchange httpExchange) throws IOException {
        final String requestUri = httpExchange.getRequestURI().toString();
        if (requestUri.startsWith("/sse/jobstats")) {
            return new JobStatsSseExchange(httpExchange, storageProvider, jsonMapper);
        } else if (requestUri.startsWith("/sse/servers")) {
            return new BackgroundJobServerStatusSseExchange(httpExchange, storageProvider, jsonMapper);
        } else if (requestUri.startsWith("/sse/jobs/")) {
            return new JobSseExchange(httpExchange, storageProvider, jsonMapper);
        }
        throw new IllegalStateException("Unsupported httpExchange");
    }

}
