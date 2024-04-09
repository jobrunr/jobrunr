package org.jobrunr.tests.e2e;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Disabled
//@Testcontainers
public class CypressJsonbUIE2ETest {

    @Container
    public SimpleBackgroundJobContainer backgroundJobContainer = new SimpleBackgroundJobContainer("--pause --jsonb");

    @Container
    public CypressTestContainer cypressContainer = new CypressTestContainer(backgroundJobContainer);

    @Test
    void runUITests() {
        await()
                .pollInterval(5, TimeUnit.SECONDS)
                .atMost(6, TimeUnit.MINUTES).untilAsserted(() -> assertThat(cypressContainer.getLogs()).contains("(Run Finished)"));

        assertThat(cypressContainer.getLogs())
                .describedAs("UI Tests failed: \n\n" + cypressContainer.getLogs())
                .contains("All specs passed!");

        System.out.println("UI Tests succeeded:\n\n" + cypressContainer.getLogs());
    }

}
