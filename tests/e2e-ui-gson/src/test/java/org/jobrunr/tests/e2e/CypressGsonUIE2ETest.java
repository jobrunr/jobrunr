package org.jobrunr.tests.e2e;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CypressGsonUIE2ETest {

    @Container
    public SimpleBackgroundJobContainer backgroundJobContainer = new SimpleBackgroundJobContainer()
            .withCommand("--pause");

    @Container
    public CypressTestContainer cypressContainer = new CypressTestContainer(backgroundJobContainer);

    @Test
    public void runUITests() throws IOException {
        assertThat(cypressContainer.getLogs())
                .describedAs("UI Tests failed: \n\n" + cypressContainer.getLogs())
                .contains("All specs passed!");

        System.out.println("UI Tests succeeded:\n\n" + cypressContainer.getLogs());
    }

}
