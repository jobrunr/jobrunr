package org.jobrunr.tests.fromhost;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.testcontainers.images.PullPolicy;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// why: we create a build of the current gradle module inside docker container for each JDK
// we do not want to run this test within the docker container itself as it would otherwise run recursively
// once inside the docker build, the ENV variable JDK_TEST is set
// the end result is that only the tests inside org.jobrunr.tests.e2e must run (on the correct JDK)
//@RunTestBetween(from = "00:00", to = "03:00")
@DisabledIfEnvironmentVariable(named = "JDK_TEST", matches = "true")
class JdkTest {

    @Test
    void jdk8OpenJdk() {
        assertThat(buildAndTestOnImage("openjdk:8-jdk-slim", "52.0")).contains("BUILD SUCCESS");
    }

    @Test
    void jdk11OpenJdk() {
        assertThat(buildAndTestOnImage("openjdk:11-jdk-slim", "55.0")).contains("BUILD SUCCESS");
    }

    @Test
    void jdk17OpenJDK() {
        assertThat(buildAndTestOnImage("openjdk:17-jdk-slim", "61.0"))
                .contains("BUILD SUCCESS")
                .contains("ThreadManager of type 'ScheduledThreadPool' started");
    }

    @Test
    void jdk21EclipseTemurin() {
        assertThat(buildAndTestOnImage("eclipse-temurin:21", "65.0"))
                .contains("BUILD SUCCESS")
                .contains("ThreadManager of type 'VirtualThreadPerTask' started");
    }

    @Test
    void jdk21GraalVM() {
        assertThat(buildAndTestOnImage("ghcr.io/graalvm/graalvm-community:21", "65.0"))
                .contains("BUILD SUCCESS")
                .contains("ThreadManager of type 'VirtualThreadPerTask' started");
    }

    @Test
    void jdk25OpenJDK() {
        assertThat(buildAndTestOnImage("openjdk:25", "69.0"))
                .contains("BUILD SUCCESS")
                .contains("ThreadManager of type 'VirtualThreadPerTask' started");
    }

    private String buildAndTestOnImage(String dockerfile, String javaClassVersion) {
        final MavenBuildAndTestContainer buildAndTestContainer = new MavenBuildAndTestContainer(dockerfile);
        buildAndTestContainer
                .withImagePullPolicy(PullPolicy.ageBased(Duration.ofDays(14)))
                .withEnv("JAVA_CLASS_VERSION", javaClassVersion)
                .withStartupTimeout(Duration.ofMinutes(2))
                .start();
        return buildAndTestContainer.getLogs();
    }
}
