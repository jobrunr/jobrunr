package org.jobrunr.dashboard;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.jobrunr.dashboard.ui.model.VersionUIModel;
import org.jobrunr.dashboard.ui.model.problems.SevereJobRunrExceptionProblem;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.Paging;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.FreePortFinder;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;

abstract class JobRunrDashboardWebServerTest {

    private StorageProvider storageProvider;

    private JobRunrDashboardWebServer dashboardWebServer;
    private TeenyHttpClient http;

    abstract JsonMapper getJsonMapper();

    @BeforeEach
    void setUpWebServer() {
        final JsonMapper jsonMapper = getJsonMapper();

        storageProvider = new InMemoryStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));

        int port = FreePortFinder.nextFreePort(8000);
        dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper, port);
        dashboardWebServer.start();

        http = new TeenyHttpClient("http://localhost:" + port);
    }

    @AfterEach
    void stopWebServer() {
        dashboardWebServer.stop();
        storageProvider.close();
    }

    @Test
    void testGetVersion() {
        var getResponse = http.get("/api/version");
        // Deserialization of UI objects is not supported in every mapper
        // This tests whether the response body is valid JSON
        var version = new JacksonJsonMapper().deserialize(getResponse.body(), VersionUIModel.class);

        assertThat(getResponse.statusCode()).isEqualTo(200);
        assertThat(version.isAllowAnonymousDataUsage()).isFalse();
    }

    @Test
    void testGetJobById_ForEnqueuedJob() {
        final Job job = anEnqueuedJob().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse).hasStatusCode(200);
    }

    @Test
    void testGetJobById_ForFailedJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getJobById_ForFailedJob.json");
    }

    @Test
    void testRequeueJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> requeueResponse = http.post("/api/jobs/%s/requeue", savedJob.getId());
        assertThat(requeueResponse).hasStatusCode(204);

        assertThat(storageProvider.getJobById(job.getId())).hasState(StateName.ENQUEUED);
    }

    @Test
    void testRequeueJob_JobNotFoundReturns404() {
        HttpResponse<String> requeueResponse = http.post("/api/jobs/%s/requeue", randomUUID());
        assertThat(requeueResponse).hasStatusCode(404);
    }

    @Test
    void testDeleteJob() {
        final Job job = aFailedJobWithRetries().build();
        final Job savedJob = storageProvider.save(job);

        HttpResponse<String> deleteResponse = http.delete("/api/jobs/%s", savedJob.getId());
        assertThat(deleteResponse).hasStatusCode(204);

        HttpResponse<String> getResponse = http.get("/api/jobs/%s", savedJob.getId());
        assertThat(getResponse).hasStatusCode(200);
        assertThat(storageProvider.getJobById(savedJob.getId())).hasState(StateName.DELETED);
    }

    @Test
    void testDeleteJob_JobNotFoundReturns404() {
        HttpResponse<String> deleteResponse = http.delete("/api/jobs/%s", randomUUID());
        assertThat(deleteResponse).hasStatusCode(404);
    }

    @Test
    void testGetJobById_JobNotFoundReturns404() {
        HttpResponse<String> getResponse = http.get("/api/jobs/%s", randomUUID());
        assertThat(getResponse).hasStatusCode(404);
    }

    @Test
    void testFindJobsByState() {
        storageProvider.save(anEnqueuedJob().build());

        HttpResponse<String> getResponse = http.get("/api/jobs?state=ENQUEUED");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/findJobsByState.json");
    }

    @Test
    void testGetProblems() {
        storageProvider.save(aJob().withJobDetails(methodThatDoesNotExistJobDetails()).withState(new ScheduledState(Instant.now().plus(1, DAYS))).build());

        HttpResponse<String> getResponse = http.get("/api/problems");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/problems-job-not-found.json");
    }

    @Test
    void testDeleteProblem() {
        storageProvider.saveMetadata(new JobRunrMetadata(SevereJobRunrException.class.getSimpleName(), "some id", "some value"));

        HttpResponse<String> getResponseBeforeDelete = http.get("/api/problems");
        assertThat(getResponseBeforeDelete)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/problems-severe-jobrunr-problem.json");


        http.delete("/api/problems/" + SevereJobRunrExceptionProblem.PROBLEM_TYPE);
        HttpResponse<String> getResponseAfterDelete = http.get("/api/problems");
        assertThat(getResponseAfterDelete)
                .hasStatusCode(200)
                .hasJsonBody("[]");
    }

    @Test
    void testDeleteProblem_UnknownProblemReturns500() {
        HttpResponse<String> deleteResponse = http.delete("/api/problems/unknown-problem-type");
        assertThat(deleteResponse).hasStatusCode(500);
    }

    @Test
    void testGetRecurringJobs() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> getResponse = http.get("/api/recurring-jobs");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getRecurringJobs.json");
    }

    @Test
    void testTriggerRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());

        HttpResponse<String> getResponse = http.post("/api/recurring-jobs/recurring-job-1/trigger");
        assertThat(getResponse).hasStatusCode(204);

        List<Job> jobs = storageProvider.getJobs(StateName.ENQUEUED, Paging.OffsetBasedPage.ascOnUpdatedAt(10)).getItems();
        assertThat(jobs).hasSize(1);
    }

    @Test
    void testTriggerRecurringJob_JobNotFoundReturns404() {
        HttpResponse<String> getResponse = http.post("/api/recurring-jobs/unknown-recurring-job/trigger");
        assertThat(getResponse).hasStatusCode(404);
    }

    @Test
    void testDeleteRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());
        assertThat(storageProvider.getRecurringJobs()).hasSize(2);

        HttpResponse<String> deleteResponse = http.delete("/api/recurring-jobs/%s", "recurring-job-1");
        assertThat(deleteResponse).hasStatusCode(204);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
    }

    @Test
    void testDeleteRecurringJob_JobNotFoundReturns404() {
        HttpResponse<String> deleteResponse = http.delete("/api/recurring-jobs/unknown-recurring-job");
        assertThat(deleteResponse).hasStatusCode(404);
    }

    @Test
    void testGetBackgroundJobServers() {
        final BackgroundJobServerStatus serverStatus = aDefaultBackgroundJobServerStatus().withIsStarted().build();
        storageProvider.announceBackgroundJobServer(serverStatus);

        HttpResponse<String> getResponse = http.get("/api/servers");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getBackgroundJobServers.json");
    }
}