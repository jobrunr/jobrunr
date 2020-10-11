package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.RestHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.problems.Problems;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobRunrApiHandler extends RestHttpHandler {

    private static Problems problems;

    public JobRunrApiHandler(StorageProvider storageProvider, JsonMapper jsonMapper) {
        super("/api", jsonMapper);

        get("/jobs", findJobByState(storageProvider));

        get("/jobs/:id", getJobById(storageProvider));
        delete("/jobs/:id", deleteJobById(storageProvider));
        post("/jobs/:id/requeue", requeueJobById(storageProvider));

        get("/problems", getProblems(storageProvider));

        get("/recurring-jobs", getRecurringJobs(storageProvider));
        delete("/recurring-jobs/:id", deleteRecurringJob(storageProvider));
        post("/recurring-jobs/:id/trigger", triggerRecurringJob(storageProvider));

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

    private HttpRequestHandler requeueJobById(StorageProvider storageProvider) {
        return (request, response) -> {
            final Job job = storageProvider.getJobById(request.param(":id", UUID.class));
            job.enqueue();
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler findJobByState(StorageProvider storageProvider) {
        return (request, response) ->
                response.asJson(
                        storageProvider.getJobPage(
                                request.queryParam("state", StateName.class, StateName.ENQUEUED),
                                request.fromQueryParams(PageRequest.class)
                        ));
    }

    private HttpRequestHandler getProblems(StorageProvider storageProvider) {
        return (request, response) -> {
            if (problems == null) {
                problems = new Problems(storageProvider);
            }
            response.asJson(problems);
        };
    }

    private HttpRequestHandler getRecurringJobs(StorageProvider storageProvider) {
        return (request, response) -> {
            final List<RecurringJobUIModel> recurringJobUIModels = storageProvider
                    .getRecurringJobs()
                    .stream()
                    .map(RecurringJobUIModel::new)
                    .collect(Collectors.toList());
            response.asJson(recurringJobUIModels);
        };
    }

    private HttpRequestHandler deleteRecurringJob(StorageProvider storageProvider) {
        return (request, response) -> {
            storageProvider.deleteRecurringJob(request.param(":id"));
            response.statusCode(204);
        };
    }

    private HttpRequestHandler triggerRecurringJob(StorageProvider storageProvider) {
        return (request, response) -> {
            final RecurringJob recurringJob = storageProvider.getRecurringJobs()
                    .stream()
                    .filter(rj -> request.param(":id").equals(rj.getId()))
                    .findFirst()
                    .orElseThrow(() -> new JobNotFoundException(request.param(":id")));

            final Job job = recurringJob.toEnqueuedJob();
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler getBackgroundJobServers(StorageProvider storageProvider) {
        return (request, response) -> response.asJson(storageProvider.getBackgroundJobServers());
    }
}
