package org.jobrunr.dashboard.sse;

import com.sun.net.httpserver.HttpExchange;
import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobId;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.utils.mapper.JsonMapper;

import java.io.IOException;

import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public class JobSseExchange extends AbstractObjectSseExchange implements JobChangeListener {

    private final StorageProvider storageProvider;
    private final JobId jobId;

    public JobSseExchange(HttpExchange httpExchange, JsonMapper jsonMapper, StorageProvider storageProvider) throws IOException {
        super(httpExchange, jsonMapper);
        this.storageProvider = storageProvider;
        this.jobId = getJobId(httpExchange);
        storageProvider.addJobStorageOnChangeListener(this);
    }

    @Override
    public JobId getJobId() {
        return jobId;
    }

    @Override
    public void onChange(Job job) {
        sendObject(job);
        if (job.hasState(SUCCEEDED)) {
            close();
        }
    }

    @Override
    public void close() {
        storageProvider.removeJobStorageOnChangeListener(this);
        super.close();
    }

    private static JobId getJobId(HttpExchange httpExchange) {
        final String url = httpExchange.getRequestURI().toString();
        return JobId.parse(url.substring("/sse/jobs/".length()));
    }
}
