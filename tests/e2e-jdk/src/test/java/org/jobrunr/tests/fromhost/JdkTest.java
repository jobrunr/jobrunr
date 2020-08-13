package org.jobrunr.tests.fromhost;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.junit.jupiter.executioncondition.RunTestBetween;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

// why: we create a build of the current gradle module inside docker container for each JDK
// we do not want to run this test within the docker container itself as it would otherwise run recursively
// once inside the docker build, the ENV variable JDK_TEST is set
// the end result is that only the tests inside org.jobrunr.tests.e2e must run (on the correct JDK)
@RunTestBetween(from = "00:00", to = "03:00")
@DisabledIfEnvironmentVariable(named = "JDK_TEST", matches = "true")
public class JdkTest {

    @Test
    public void jdk8OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:8-jdk-hotspot")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8OpenJ9() {
        assertThat(buildAndTestOnImage("adoptopenjdk:8-jdk-openj9")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8Zulu() {
        assertThat(buildAndTestOnImage("azul/zulu-openjdk:8")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8GraalVM() {
        assertThat(buildAndTestOnImage("oracle/graalvm-ce:20.1.0-java8")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk8Ibm() {
        assertThat(buildAndTestOnImage("ibmcom/ibmjava:8-sdk-alpine")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:11-jdk-hotspot")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11OpenJ9() {
        assertThat(buildAndTestOnImage("adoptopenjdk:11-jdk-openj9")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11Zulu() {
        assertThat(buildAndTestOnImage("azul/zulu-openjdk:11")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk11GraalVM() {
        assertThat(buildAndTestOnImage("oracle/graalvm-ce:20.1.0-java11")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk14OpenJdk() {
        assertThat(buildAndTestOnImage("adoptopenjdk:14-jdk-hotspot")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk14OpenJ9() {
        assertThat(buildAndTestOnImage("adoptopenjdk:14-jdk-openj9")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk14Zulu() {
        assertThat(buildAndTestOnImage("azul/zulu-openjdk:14")).contains("BUILD SUCCESSFUL");
    }

    @Test
    public void jdk15OpenJDKEA() {
        assertThat(buildAndTestOnImage("amd64/openjdk:15-ea-jdk")).contains("BUILD SUCCESSFUL");
    }

    @Disabled("Not yet working due to gradle")
    @Test
    public void jdk16OpenJDKEA() {
        assertThat(buildAndTestOnImage("amd64/openjdk:16-ea-jdk")).contains("BUILD SUCCESSFUL");
    }

    private String buildAndTestOnImage(String dockerfile) {
        final BuildAndTestContainer buildAndTestContainer = new BuildAndTestContainer(dockerfile);
        buildAndTestContainer
                .withStartupTimeout(Duration.ofMinutes(5))
                .start();
        return buildAndTestContainer.getLogs();
    }
}
