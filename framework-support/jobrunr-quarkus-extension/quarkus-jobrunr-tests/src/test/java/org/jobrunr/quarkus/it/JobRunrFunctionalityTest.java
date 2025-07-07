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

    private final TeenyHttpClient restApi = new TeenyHttpClient("http://localhost:8081");

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
                .hasBodyContaining("id=another-recurring-job-with-jobContext", "jobSignature='org.jobrunr.quarkus.it.TestService.anotherRecurringJob(org.jobrunr.jobs.context.JobContext)'");
//                .hasJsonBody(json -> json.inPath("[1].id").isEqualTo("another-recurring-job-with-jobContext"))
//                .hasJsonBody(json -> json.inPath("[1].name").isEqualTo("Doing some work with the job context"))
//                .hasJsonBody(json -> json.inPath("[1].scheduleExpression").isEqualTo("PT10M"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.className").isEqualTo("org.jobrunr.quarkus.it.TestService"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.methodName").isEqualTo("aRecurringJob"))
//                .hasJsonBody(json -> json.inPath("[1].jobDetails.methodName").isEqualTo("aRecurringJob"));
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
                .hasBodyContaining("jobrunr_jobs_by_state{state=\"ENQUEUED\"}")
                .hasBodyContaining("jobrunr_jobs_by_state{state=\"SUCCEEDED\"}");
    }
}
