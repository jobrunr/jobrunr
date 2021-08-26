package org.jobrunr.quarkus.it;

import io.quarkus.test.junit.QuarkusTest;
import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.StringUtils.substringAfter;


@QuarkusTest
@DisplayName("Tests JobRunr Quarkus extension")
public class JobRunrFunctionalityTest {

    private final TeenyHttpClient dashboardApi = new TeenyHttpClient("http://localhost:8000");
    private final TeenyHttpClient restApi = new TeenyHttpClient("http://localhost:8081");

    @Test
    public void testDashboard() {
        final HttpResponse<String> response = dashboardApi.get("/api/servers");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyContaining("firstHeartbeat", "\"running\":true", "\"workerPoolSize\":10");
    }

    @Test
    public void testEnqueueAndProcessJob() {
        final HttpResponse<String> response = restApi.post("/jobrunr/jobs");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyStartingWith("Job Enqueued:");

        await().untilAsserted(() -> assertThat(dashboardApi.get("/api/jobs/" + substringAfter(response.body(), "Job Enqueued: ")))
                .hasBodyContaining("SUCCEEDED"));
    }

    @Test
    public void testRecurringJobs() {
        final HttpResponse<String> response = dashboardApi.get("/api/recurring-jobs");
        assertThat(response)
                .hasStatusCode(200)
                .hasJsonBody(json -> json.inPath("[0].cronExpression").isEqualTo("*/15 * * * *"))
                .hasJsonBody(json -> json.inPath("[0].id").isEqualTo("my-recurring-job"))
                .hasJsonBody(json -> json.inPath("[0].jobDetails.className").isEqualTo("org.jobrunr.quarkus.it.TestService"))
                .hasJsonBody(json -> json.inPath("[0].jobDetails.methodName").isEqualTo("aRecurringJob"));
    }

    @Test
    public void testJobRunrHealthCheck() {
        final HttpResponse<String> response = restApi.get("/q/health/ready");
        assertThat(response)
                .hasStatusCode(200)
                .hasJsonBody(json -> json.inPath("checks[0].name").isEqualTo("JobRunr"))
                .hasJsonBody(json -> json.inPath("checks[0].status").isEqualTo("UP"));
    }

    @Test
    public void testJobRunrMetrics() {
        final HttpResponse<String> response = restApi.get("/q/metrics");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyContaining("jobrunr_jobs_succeeded")
                .hasBodyContaining("jobrunr_jobs_enqueued");
    }
}
