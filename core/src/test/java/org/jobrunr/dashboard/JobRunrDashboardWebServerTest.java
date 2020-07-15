package org.jobrunr.dashboard;

import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static java.util.UUID.randomUUID;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;

abstract class JobRunrDashboardWebServerTest {

    private SimpleStorageProvider storageProvider;

    private JobRunrDashboardWebServer dashboardWebServer;
    private TeenyHttpClient http;

    @BeforeEach
    void setUpWebServer() {
        final JsonMapper jsonMapper = getJsonMapper();

        storageProvider = new SimpleStorageProvider();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));
        dashboardWebServer = new JobRunrDashboardWebServer(storageProvider, jsonMapper);

        http = new TeenyHttpClient("http://localhost:8000");
    }

    abstract JsonMapper getJsonMapper();

    @AfterEach
    void stopWebServer() {
        dashboardWebServer.stop();
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

        HttpResponse<String> deleteResponse = http.post("/api/jobs/%s/requeue", savedJob.getId());
        assertThat(deleteResponse).hasStatusCode(204);

        assertThat(storageProvider.getJobById(job.getId())).hasState(StateName.ENQUEUED);
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
    void testGetRecurringJobs() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> getResponse = http.get("/api/recurring-jobs");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getRecurringJobs.json");
    }

    @Test
    void testDeleteRecurringJob() {
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-1").withName("Import sales data").build());
        storageProvider.saveRecurringJob(aDefaultRecurringJob().withId("recurring-job-2").withName("Generate sales reports").build());

        HttpResponse<String> deleteResponse = http.delete("/api/recurring-jobs/%s", "recurring-job-1");
        assertThat(deleteResponse).hasStatusCode(204);
        assertThat(storageProvider.getRecurringJobs()).hasSize(1);
    }

    @Test
    void testGetBackgroundJobServers() {
        storageProvider.announceBackgroundJobServer(new BackgroundJobServerStatus(15, 10));

        HttpResponse<String> getResponse = http.get("/api/servers");
        assertThat(getResponse)
                .hasStatusCode(200)
                .hasSameJsonBodyAsResource("/dashboard/api/getBackgroundJobServers.json");
    }
}