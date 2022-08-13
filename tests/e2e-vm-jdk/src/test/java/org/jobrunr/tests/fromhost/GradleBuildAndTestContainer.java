package org.jobrunr.tests.fromhost;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;

import static java.nio.file.Files.exists;

public class GradleBuildAndTestContainer extends GenericContainer<GradleBuildAndTestContainer> {

    public GradleBuildAndTestContainer(String fromDockerImage) {
        super(new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                        builder
                                .from(fromDockerImage)
                                .workDir("/app/jobrunr")
                                .env("JDK_TEST", "true")
                ));
        if (exists(Paths.get("/drone"))) {
            this
                    .withFileSystemBind(Paths.get("/tmp/jobrunr/cache/gradle-wrapper").toString(), "/root/.gradle/wrapper/dists");
        } else {
            this
                    .withFileSystemBind(Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists").toString(), "/root/.gradle/wrapper/dists");
        }

        this
                .withCopyFileToContainer(MountableFile.forHostPath(Paths.get(".")), "/app/jobrunr")
                .withCommand("./gradlew", "build")
                .waitingFor(Wait.forLogMessage(".*BUILD SUCCESSFUL.*|.*BUILD FAILED.*|.*FAILURE: Build failed.*|.*BUILD FAILURE.*", 1));
    }
}
