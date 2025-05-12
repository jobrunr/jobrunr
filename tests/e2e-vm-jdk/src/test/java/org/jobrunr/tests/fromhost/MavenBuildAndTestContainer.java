package org.jobrunr.tests.fromhost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;

import static java.nio.file.Files.exists;

public class MavenBuildAndTestContainer extends GenericContainer<MavenBuildAndTestContainer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenBuildAndTestContainer.class);

    public MavenBuildAndTestContainer(String fromDockerImage) {
        super(new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                        builder
                                .from(fromDockerImage)
                                .workDir("/app/jobrunr")
                                .env("JDK_TEST", "true")
                ));

        if (exists(Paths.get("/drone"))) {
            LOGGER.info("Running inside CI / Drone Build Container (DRONE_WORK_DIR={})", System.getenv("DRONE_WORK_DIR"));
            this
                    .withFileSystemBind(System.getenv("DRONE_WORK_DIR") + "/m2/cache", "/root/.m2", BindMode.READ_WRITE);
        } else {
            LOGGER.info("Running on developer machine");
            this
                    .withFileSystemBind(Paths.get(System.getProperty("user.home"), ".m2").toString(), "/root/.m2", BindMode.READ_WRITE);
        }

        this
                .withCopyFileToContainer(MountableFile.forHostPath(Paths.get(".")), "/app/jobrunr")
                .withCommand("./mvnw", "clean", "install")
                .waitingFor(Wait.forLogMessage(".*BUILD SUCCESS.*|.*BUILD FAILED.*|.*FAILURE: Build failed.*|.*BUILD FAILURE.*|.*Error: Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain.*", 1));
    }
}
