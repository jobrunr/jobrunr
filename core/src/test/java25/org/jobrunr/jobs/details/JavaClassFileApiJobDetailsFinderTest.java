package org.jobrunr.jobs.details;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.junit.jupiter.api.Test;

import java.lang.invoke.SerializedLambda;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.getJavaClassContainingLambdaAsInputStream;

class JavaClassFileApiJobDetailsFinderTest {

    @Test
    void itDoesNotWrapTheParserErrorForAClassFileNewerThanTheRunningJdk() throws Exception {
        JobLambda lambda = () -> System.out.println("test");
        SerializedLambda serializedLambda = SerializedLambdaConverter.toSerializedLambda(lambda);
        byte[] classBytes = getJavaClassContainingLambdaAsInputStream(lambda).readAllBytes();
        classBytes[6] = 0;
        classBytes[7] = 100; // major version 100, far beyond any released JDK

        JavaClassFileApiJobDetailsFinder finder = new JavaClassFileApiJobDetailsFinder(lambda, serializedLambda);
        assertThatThrownBy(() -> finder.parse(serializedLambda.getImplMethodName(), classBytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported class file version");
    }
}
