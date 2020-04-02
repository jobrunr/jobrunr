package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.RestHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.UUID;

public class JobRunrApiHandler extends RestHttpHandler {

    public JobRunrApiHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        super("/api", jsonMapper);

        get("/jobs/:id", getJobById(storageProvider));
        delete("/jobs/:id", deleteJobById(storageProvider));

        get("/jobs/default/:state", findJobByState(storageProvider));

        get("/servers", getBackgroundJobServers(storageProvider));

        withExceptionMapping(JobNotFoundException.class, (exc, resp) -> resp.statusCode(404));
    }

    private HttpRequestHandler getJobById(StorageProvider storageProvider) {
        return (request, response) -> response.asJson(storageProvider.getJobById(request.param(":id", UUID.class)));
    }

    private HttpRequestHandler deleteJobById(StorageProvider storageProvider) {
        return (request, response) -> {
            storageProvider.delete(request.param(":id", UUID.class));
            response.statusCode(204);
        };
    }

    private HttpRequestHandler findJobByState(StorageProvider storageProvider) {
        return (request, response) ->
                response.asJson(
                        storageProvider.getJobPage(
                                request.param(":state", StateName.class),
                                request.fromQueryParams(PageRequest.class)
                        ));
    }

    private HttpRequestHandler getBackgroundJobServers(StorageProvider storageProvider) {
        return (request, response) ->
                response.asJson(storageProvider.getBackgroundJobServers());
    }
}
