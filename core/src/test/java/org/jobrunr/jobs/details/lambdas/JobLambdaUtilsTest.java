package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.isKotlinClassBasedLambda;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.serializedLambda;

class JobLambdaUtilsTest {

    @Test
    void javaLambdaMatchesOnlyTheSyntheticLambdaMethod() {
        JobLambda jobLambda = () -> System.out.println("test");
        SerializedJobLambda resolvedLambda = serializedLambda(jobLambda);

        assertThat(isKotlinClassBasedLambda(jobLambda)).isFalse();
        assertThat(resolvedLambda).isInstanceOf(JavaSerializedLambda.class);
        assertThat(resolvedLambda.isLambdaBody()).isTrue();
        assertThat(resolvedLambda.matches(resolvedLambda.serializedLambda.getImplMethodName())).isTrue();
        assertThat(resolvedLambda.matches("someOtherMethod")).isFalse();
    }

    @Test
    void javaLambdaMatchesNothingForAMethodReference() {
        TestService testService = new TestService();
        JobLambda jobLambda = testService::doWork;
        SerializedJobLambda resolvedLambda = serializedLambda(jobLambda);

        assertThat(resolvedLambda).isInstanceOf(JavaSerializedLambda.class);
        assertThat(resolvedLambda.isLambdaBody()).isFalse();
        assertThat(resolvedLambda.matches("doWork")).isFalse();
    }
}
