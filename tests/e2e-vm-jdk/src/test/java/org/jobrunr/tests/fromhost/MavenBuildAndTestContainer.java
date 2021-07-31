package org.jobrunr.tests.fromhost;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;

import static java.nio.file.Files.exists;

public class MavenBuildAndTestContainer extends GenericContainer<MavenBuildAndTestContainer> {

    public MavenBuildAndTestContainer(String fromDockerImage) {
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
                    .withFileSystemBind(Paths.get(System.getProperty("user.home"), ".m2").toString(), "/root/.m2");
        }

        this
                .withCopyFileToContainer(MountableFile.forHostPath(Paths.get(".")), "/app/jobrunr")
                .withCommand("./mvnw", "clean", "install")
                .waitingFor(Wait.forLogMessage(".*BUILD SUCCESS.*|.*BUILD FAILED.*|.*FAILURE: Build failed.*", 1));
    }
}
