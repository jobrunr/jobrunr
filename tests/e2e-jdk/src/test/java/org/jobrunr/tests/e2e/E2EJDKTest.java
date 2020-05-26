package org.jobrunr.tests.e2e;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.tests.e2e.HttpClient.getJson;

public class E2EJDKTest {

    public void startJobRunr() throws Exception {
        new Main(new String[]{});
    }

    @Test
    void runTests() {
        new Thread(() -> startJobRunrNoException()).start();

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    private String getSucceededJobs() {
        return getJson("http://localhost:8000/api/jobs/default/succeeded");
    }

    private void startJobRunrNoException() {
        try {
            startJobRunr();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
