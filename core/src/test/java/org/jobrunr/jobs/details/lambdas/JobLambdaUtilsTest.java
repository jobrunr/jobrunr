package org.jobrunr.jobs.details.lambdas;

import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.isKotlinClassBasedLambda;
import static org.jobrunr.jobs.details.lambdas.JobLambdaUtils.serializedLambda;
import static org.jobrunr.jobs.details.lambdas.KotlinClassBasedLambda.KotlinVersion;

class JobLambdaUtilsTest {

    @Test
    void kotlinVersionComesFromTheMetadataMvArray() {
        assertThat(KotlinVersion.from(new int[]{1, 4})).isEqualTo(KotlinVersion.ONE_FOUR);
        assertThat(KotlinVersion.from(new int[]{1, 5})).isEqualTo(KotlinVersion.ONE_FIVE);
        assertThat(KotlinVersion.from(new int[]{1, 6})).isEqualTo(KotlinVersion.ONE_SIX_OR_HIGHER);
        assertThat(KotlinVersion.from(new int[]{2, 2, 0})).isEqualTo(KotlinVersion.ONE_SIX_OR_HIGHER);
    }

    @Test
    void unsupportedKotlinVersionsAreRejectedWithTheVersionInTheMessage() {
        assertThatThrownBy(() -> KotlinVersion.from(new int[]{1, 3}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("The Kotlin version 1.3 is unsupported");
    }

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
