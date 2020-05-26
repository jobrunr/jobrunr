package org.jobrunr.tests.fromhost;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import java.io.File;
import java.io.IOException;

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
        dockerClient = DockerClientBuilder.getInstance().build();
    }

    @Test
    public void jdk8OpenJdk() {
        assertThat(buildImage("./Dockerfile-8-openjdk")).isNotEmpty();
    }

    @Test
    public void jdk8OpenJ9() {
        assertThat(buildImage("./Dockerfile-8-openj9")).isNotEmpty();
    }

    @Test
    public void jdk8GraalVM() {
        assertThat(buildImage("./Dockerfile-8-graalvm")).isNotEmpty();
    }

    private String buildImage(String dockerfile) {
        return dockerClient.buildImageCmd()
                .withDockerfile(new File(dockerfile))
                .withPull(true)
                .exec(new BuildImageResultCallback())
                .awaitImageId();
    }
}
