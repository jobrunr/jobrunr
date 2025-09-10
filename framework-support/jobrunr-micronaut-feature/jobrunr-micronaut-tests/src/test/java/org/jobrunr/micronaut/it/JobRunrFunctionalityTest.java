package org.jobrunr.micronaut.it;

import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.StringUtils.substringAfter;

@MicronautTest(rebuildContext = true)
public class JobRunrFunctionalityTest {

    TeenyHttpClient restApi;

    @Inject
    EmbeddedServer embeddedServer;

    @BeforeEach
    void setUpRestApi() {
        restApi = new TeenyHttpClient("http://localhost:" + embeddedServer.getPort());
    }

    @Test
    public void testEnqueueAndProcessJob() {
        final HttpResponse<String> response = restApi.post("/jobrunr/jobs");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyStartingWith("Job Enqueued:");

        await().untilAsserted(() -> assertThat(restApi.get("/jobrunr/jobs/" + substringAfter(response.body(), "Job Enqueued: ")))
                .hasBodyContaining("SUCCEEDED"));
    }

    @Test
    public void testRecurringJobs() {
        final HttpResponse<String> response = restApi.get("/jobrunr/recurring-jobs");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyContaining("my-recurring-job", "org.jobrunr.micronaut.it.TestService.aRecurringJob()")
                .hasBodyContaining("another-recurring-job-with-jobContext", "org.jobrunr.micronaut.it.TestService.anotherRecurringJob(org.jobrunr.jobs.context.JobContext)");
    }
}
