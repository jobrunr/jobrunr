package org.jobrunr.quarkus.it;

import io.quarkus.test.junit.QuarkusTest;
import org.jobrunr.dashboard.server.http.client.TeenyHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;

import static org.jobrunr.JobRunrAssertions.assertThat;


@QuarkusTest
@DisplayName("Tests JobRunr extension")
public class JobRunrFunctionalityTest {

    private TeenyHttpClient http = new TeenyHttpClient("http://localhost:8081");

    @Test
    public void testJobRunrFunctionality() throws InterruptedException {
        final HttpResponse<String> response = http.get("/jobrunr/enqueue");
        assertThat(response)
                .hasStatusCode(200)
                .hasBodyStartingWith("Job Enqueued:");
    }


}
