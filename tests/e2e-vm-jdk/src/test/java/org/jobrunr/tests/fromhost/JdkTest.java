package org.jobrunr.tests.fromhost;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.testcontainers.images.PullPolicy;

import java.time.Duration;

import static java.lang.System.getProperty;
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
        assertThat(buildAndTestOnImage("adoptopenjdk:8-jdk-hotspot", "52.0")).contains("BUILD SUCCESS");
    }

    @Test
    void jdk11OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:11-jdk-hotspot", "55.0")).contains("BUILD SUCCESS");
    }

    @Test
    void jdk17OpenJDK() {
        assertThat(buildAndTestOnImage(architecture() + "/openjdk:17", "61.0"))
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
    void jdk24OpenJDK() {
        assertThat(buildAndTestOnImage("openjdk:24", "68.0"))
                .contains("BUILD SUCCESS")
                .contains("ThreadManager of type 'VirtualThreadPerTask' started");
    }

    private String buildAndTestOnImage(String dockerfile, String javaClassVersion) {
        final MavenBuildAndTestContainer buildAndTestContainer = new MavenBuildAndTestContainer(dockerfile);
        try {
            buildAndTestContainer
                    .withImagePullPolicy(PullPolicy.ageBased(Duration.ofDays(14)))
                    .withEnv("JAVA_CLASS_VERSION", javaClassVersion)
                    .withStartupTimeout(Duration.ofMinutes(2))
                    .start();
        } finally {
            String logs = buildAndTestContainer.getLogs();
            //System.out.println(logs);
            return logs;
        }
    }

    private static String architecture() {
        return "aarch64".equals(getProperty("os.arch")) ? "arm64v8" : "amd64";
    }
}
