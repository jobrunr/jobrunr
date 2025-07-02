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

@MicronautTest
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
                .hasBodyContaining("id=my-recurring-job", "jobSignature='org.jobrunr.quarkus.it.TestService.aRecurringJob()'")
                .hasBodyContaining("id=another-recurring-job-with-jobContext", "jobSignature='org.jobrunr.quarkus.it.TestService.aRecurringJob()'");
//                .hasJsonBody(json -> json.inPath("[1].id").isEqualTo("another-recurring-job-with-jobContext"))
//                .hasJsonBody(json -> json.inPath("[1].name").isEqualTo("Doing some work with the job context"))
//                .hasJsonBody(json -> json.inPath("[1].scheduleExpression").isEqualTo("PT10M"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.className").isEqualTo("org.jobrunr.quarkus.it.TestService"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.methodName").isEqualTo("aRecurringJob"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.methodName").isEqualTo("aRecurringJob"));
    }
}
