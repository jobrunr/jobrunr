package org.jobrunr.tests.fromhost;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Stream;

import static java.nio.file.Files.exists;

public class BuildAndTestContainer extends GenericContainer<BuildAndTestContainer> {

    public BuildAndTestContainer(String fromDockerImage) {
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
                .withStartupTimeout(Duration.ofMinutes(10))
                .waitingFor(Wait.forLogMessage(".*BUILD SUCCESSFUL.*", 1));
    }

    private void listFiles(Path gradleDistPath) {
        System.out.println("============================================================");
        System.out.println("====================   Files     ===========================");
        System.out.println("============================================================");
        System.out.println("Path: " + gradleDistPath.toString());
        try (Stream<Path> stream = Files.walk(gradleDistPath, 2)) {
            stream
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
