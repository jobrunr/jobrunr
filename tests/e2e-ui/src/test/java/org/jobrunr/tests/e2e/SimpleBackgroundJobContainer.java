package org.jobrunr.tests.e2e;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SimpleBackgroundJobContainer extends GenericContainer<SimpleBackgroundJobContainer> {

    public SimpleBackgroundJobContainer(String command) {
        super("jobrunr-e2e-ui:1.0");
        this
                .withCommand(command)
                .withNetworkMode("host")
                .waitingFor(Wait.forLogMessage(".*Background Job server is ready *\\n", 1));
    }

}
