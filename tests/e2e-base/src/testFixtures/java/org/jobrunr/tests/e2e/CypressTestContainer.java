package org.jobrunr.tests.e2e;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

public class CypressTestContainer extends GenericContainer<CypressTestContainer> {

    private final GenericContainer backgroundJobContainer;

    public CypressTestContainer(GenericContainer backgroundJobContainer) {
        super("cypress/included:12.11.0");
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
                    .withStartupTimeout(Duration.ofMinutes(5));

            if (Files.exists(Paths.get("/drone"))) {
                this
                        .withCopyFileToContainer(MountableFile.forClasspathResource("/org/jobrunr/dashboard/frontend"), "/e2e")
                        .withCopyFileToContainer(MountableFile.forHostPath(new File("/drone/core/src/main/resources/org/jobrunr/dashboard/frontend/cypress").getPath()), "/e2e/cypress")
                        .withCopyFileToContainer(MountableFile.forHostPath(new File("/drone/core/src/main/resources/org/jobrunr/dashboard/frontend/cypress.config.js").getPath()), "/e2e/cypress.config.js");
            } else {
                this
                        .withClasspathResourceMapping("/org/jobrunr/dashboard/frontend", "/e2e", BindMode.READ_WRITE)
                        .withCopyFileToContainer(MountableFile.forHostPath(new File("../../core/src/main/resources/org/jobrunr/dashboard/frontend/cypress").getPath()), "/e2e/cypress")
                        .withCopyFileToContainer(MountableFile.forHostPath(new File("../../core/src/main/resources/org/jobrunr/dashboard/frontend/cypress.config.js").getPath()), "/e2e/cypress.config.js");
            }
            super.start();
        } catch (Exception e) {
            System.out.println(getLogs());
            throw new RuntimeException(e);
        }
    }
}
