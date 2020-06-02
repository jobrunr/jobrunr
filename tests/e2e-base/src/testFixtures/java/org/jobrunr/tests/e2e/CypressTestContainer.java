package org.jobrunr.tests.e2e;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

public class CypressTestContainer extends GenericContainer<CypressTestContainer> {

    private final GenericContainer backgroundJobContainer;

    public CypressTestContainer(GenericContainer backgroundJobContainer) {
        super("cypress/included:4.3.0");
        this.backgroundJobContainer = backgroundJobContainer;
    }

    @Override
    public void start() {
        try {
            this
                    .dependsOn(backgroundJobContainer)
                    .withNetworkMode("host")
                    .withSharedMemorySize(536870912L)
                    .withWorkingDirectory("/e2e")
                    .withStartupTimeout(Duration.ofMinutes(10))
                    .waitingFor(Wait.forLogMessage(".*(Run Finished).*", 1));

            if (Files.exists(Paths.get("/drone"))) {
                this
                        .withCopyFileToContainer(MountableFile.forClasspathResource("/org/jobrunr/dashboard/frontend"), "/e2e");
            } else {
                this
                        .withClasspathResourceMapping("/org/jobrunr/dashboard/frontend", "/e2e", BindMode.READ_WRITE);
            }
            super.start();
        } catch (Exception e) {
            System.out.println(getLogs());
            throw new RuntimeException(e);
        }
    }
}
