package org.jobrunr.tests.e2e;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SimpleBackgroundJobContainer extends GenericContainer<SimpleBackgroundJobContainer> {

    public SimpleBackgroundJobContainer() {
        super("jobrunr-e2e-ui-jackson:1.0");
        this
                .withCommand("--pause")
                .withNetworkMode("host")
                .waitingFor(Wait.forLogMessage(".*Background Job server is ready *\\n", 1));
    }

}
