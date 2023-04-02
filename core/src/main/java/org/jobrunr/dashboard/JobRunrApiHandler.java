package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.RestHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.ProblemsManager;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.*;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class JobRunrApiHandler extends RestHttpHandler {

    private final StorageProvider storageProvider;
    private final boolean allowAnonymousDataUsage;
    private ProblemsManager problemsManager;
    private RecurringJobsResult recurringJobsResult;
    private VersionUIModel versionUIModel;

    public JobRunrApiHandler(StorageProvider storageProvider, JsonMapper jsonMapper, boolean allowAnonymousDataUsage) {
        super("/api", jsonMapper);
        this.storageProvider = storageProvider;
        this.allowAnonymousDataUsage = allowAnonymousDataUsage;

        get("/problems", getProblems());
        delete("/problems/:type", deleteProblemByType());

        get("/job-signatures", getDistinctJobSignatures());

        get("/jobs", findJobByState());

        get("/jobs/:id", getJobById());
        delete("/jobs/:id", deleteJobById());
        post("/jobs/:id/requeue", requeueJobById());

        get("/recurring-jobs", getRecurringJobs());
        delete("/recurring-jobs/:id", deleteRecurringJob());
        post("/recurring-jobs/:id/trigger", triggerRecurringJob());

        get("/servers", getBackgroundJobServers());
        get("/version", getVersion());

        withExceptionMapping(JobNotFoundException.class, (exc, resp) -> resp.statusCode(404));
    }

    private HttpRequestHandler getJobById() {
        return (request, response) -> response.asJson(storageProvider.getJobById(request.param(":id", UUID.class)));
    }

    private HttpRequestHandler deleteJobById() {
        return (request, response) -> {
            final Job job = storageProvider.getJobById(request.param(":id", UUID.class));
            job.delete("Job deleted via Dashboard");
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler requeueJobById() {
        return (request, response) -> {
            final Job job = storageProvider.getJobById(request.param(":id", UUID.class));
            job.enqueue();
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler findJobByState() {
        return (request, response) ->
                response.asJson(
                        storageProvider.getJobPage(
                                request.queryParam("state", StateName.class, StateName.ENQUEUED),
                                request.fromQueryParams(PageRequest.class)
                        ));
    }

    private HttpRequestHandler getProblems() {
        return (request, response) -> {
            response.asJson(problemsManager().getProblems());
        };
    }

    private HttpRequestHandler deleteProblemByType() {
        return (request, response) -> {
            problemsManager().dismissProblemOfType(request.param(":type", String.class));
            response.statusCode(204);
        };
    }

    private HttpRequestHandler getDistinctJobSignatures() {
        return (request, response) -> response.asJson(storageProvider.getDistinctJobSignatures(StateName.values()));
    }

    private HttpRequestHandler getRecurringJobs() {
        return (request, response) -> {
            PageRequest pageRequest = request.fromQueryParams(PageRequest.class);
            RecurringJobsResult recurringJobs = recurringJobResults();
            final List<RecurringJobUIModel> recurringJobUIModels = recurringJobs
                    .stream()
                    .skip(pageRequest.getOffset())
                    .limit(pageRequest.getLimit())
                    .map(RecurringJobUIModel::new)
                    .collect(Collectors.toList());
            Page<RecurringJobUIModel> result = new Page<>(recurringJobs.size(), recurringJobUIModels, pageRequest);
            response.asJson(result);
        };
    }

    private HttpRequestHandler deleteRecurringJob() {
        return (request, response) -> {
            storageProvider.deleteRecurringJob(request.param(":id"));
            response.statusCode(204);
        };
    }

    private HttpRequestHandler triggerRecurringJob() {
        return (request, response) -> {
            final RecurringJob recurringJob = recurringJobResults()
                    .stream()
                    .filter(rj -> request.param(":id").equals(rj.getId()))
                    .findFirst()
                    .orElseThrow(() -> new JobNotFoundException(request.param(":id")));

            final Job job = recurringJob.toEnqueuedJob();
            storageProvider.save(job);
            response.statusCode(204);
        };
    }

    private HttpRequestHandler getBackgroundJobServers() {
        return (request, response) -> response.asJson(storageProvider.getBackgroundJobServers());
    }

    private HttpRequestHandler getVersion() {
        return (request, response) -> response.asJson(getVersionUIModel());
    }

    private VersionUIModel getVersionUIModel() {
        if (versionUIModel != null) return versionUIModel;
        if (allowAnonymousDataUsage) {
            final JobRunrMetadata metadata = storageProvider.getMetadata("id", "cluster");
            if (metadata != null) {
                final String storageProviderType = storageProvider instanceof ThreadSafeStorageProvider
                        ? ((ThreadSafeStorageProvider) storageProvider).getStorageProvider().getClass().getSimpleName()
                        : storageProvider.getClass().getSimpleName();
                this.versionUIModel = VersionUIModel.withAnonymousDataUsage(metadata.getValue(), storageProviderType);
                return this.versionUIModel;
            }
            // wait for background job server to add cluster id. Return no anonymous data usage for now.
            return VersionUIModel.withoutAnonymousDataUsage();
        } else {
            this.versionUIModel = VersionUIModel.withoutAnonymousDataUsage();
            return this.versionUIModel;
        }
    }

    private ProblemsManager problemsManager() {
        if (this.problemsManager == null) {
            this.problemsManager = new ProblemsManager(storageProvider);
        }
        return this.problemsManager;
    }

    private RecurringJobsResult recurringJobResults() {
        if (recurringJobsResult == null || storageProvider.recurringJobsUpdated(recurringJobsResult.getLastModifiedHash())) {
            recurringJobsResult = storageProvider.getRecurringJobs();
        }
        return recurringJobsResult;
    }
}
