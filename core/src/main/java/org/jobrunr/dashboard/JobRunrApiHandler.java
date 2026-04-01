package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.RestHttpHandler;
import org.jobrunr.dashboard.server.http.handlers.HttpRequestHandler;
import org.jobrunr.dashboard.ui.model.RecurringJobUIModel;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.ProblemsManager;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobNotFoundException;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.Page;
import org.jobrunr.storage.RecurringJobsResult;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.storage.navigation.OffsetBasedPageRequest;
import org.jobrunr.utils.mapper.JsonMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

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

        get("/metadata/:name/:owner", getMetadataByNameAndOwner());
        get("/problems", getProblems());
        delete("/problems/:type", deleteProblemByType());

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

    private HttpRequestHandler getMetadataByNameAndOwner() {
        return (request, response) -> {
            String name = request.param(":name");
            String owner = request.param(":owner");

            if (isNullOrEmpty(name) || isNullOrEmpty(owner)) {
                response.statusCode(404);
                return;
            }

            JobRunrMetadata metadata = storageProvider.getMetadata(name, owner);
            if (metadata == null) {
                response.statusCode(404);
            } else {
                String format = request.queryParam("format", String.class, null);
                if ("jsonValue".equals(format)) {
                    response.fromJsonString(metadata.getValue());
                } else {
                    response.asJson(metadata);
                }
            }
        };
    }

    private HttpRequestHandler getProblems() {
        return (request, response) -> response.asJson(problemsManager().getProblems());
    }

    private HttpRequestHandler deleteProblemByType() {
        return (request, response) -> {
            problemsManager().dismissProblemOfType(request.param(":type", String.class));
            response.statusCode(204);
        };
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
                        storageProvider.getJobs(
                                request.queryParam("state", StateName.class, StateName.ENQUEUED),
                                request.fromQueryParams(OffsetBasedPageRequest.class)
                        ));
    }

    private HttpRequestHandler getRecurringJobs() {
        return (request, response) -> {
            OffsetBasedPageRequest pageRequest = request.fromQueryParams(OffsetBasedPageRequest.class);
            RecurringJobsResult recurringJobs = recurringJobResults();
            final List<RecurringJobUIModel> recurringJobUIModels = recurringJobs
                    .stream()
                    .skip(pageRequest.getOffset())
                    .limit(pageRequest.getLimit())
                    .map(RecurringJobUIModel::new)
                    .collect(Collectors.toList());
            Page<RecurringJobUIModel> result = pageRequest.mapToNewPage(recurringJobs.size(), recurringJobUIModels);
            response.asJson(result);
        };
    }

    private HttpRequestHandler deleteRecurringJob() {
        return (request, response) -> {
            String jobId = request.param(":id");
            int deleted = storageProvider.deleteRecurringJob(jobId);
            if (deleted == 0) {
                throw new JobNotFoundException(jobId);
            }
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
