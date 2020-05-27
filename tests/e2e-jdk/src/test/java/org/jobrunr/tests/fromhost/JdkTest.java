package org.jobrunr.tests.fromhost;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.IOException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// why: we create a build of the current gradle module inside docker container for each JDK
// this is done via the different Dockerfiles and we do not want to start these tests recursively.
// once inside the docker build, the ENV variable JDK_TEST is set
// the end result is that only the tests inside org.jobrunr.tests.e2e must run (on the correct JDK)
@DisabledIfEnvironmentVariable(named = "JDK_TEST", matches = "true")
public class JdkTest {

    private static DockerClient dockerClient;

    @BeforeAll
    public static void initDockerClient() throws IOException {
        //dockerClient = DockerClientBuilder.getInstance().build();
    }

    @Test
    public void jdk8OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:8-jdk-hotspot")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8OpenJ9() {
        assertThat(buildAndTestOnImage("adoptopenjdk:8-jdk-openj9")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8GraalVM() {
        assertThat(buildAndTestOnImage("oracle/graalvm-ce:20.1.0-java8")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:11-jdk-hotspot")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11OpenJ9() {
        assertThat(buildAndTestOnImage("adoptopenjdk:11-jdk-openj9")).contains("BUILD SUCCESSFUL");
    }

    private String buildAndTestOnImage(String dockerfile) {
        final BuildAndTestContainer buildAndTestContainer = new BuildAndTestContainer(dockerfile);
        buildAndTestContainer
                .withStartupTimeout(Duration.ofMinutes(10))
                .start();
        return buildAndTestContainer.getLogs();
    }
}
