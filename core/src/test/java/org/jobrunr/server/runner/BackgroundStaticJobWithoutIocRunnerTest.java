package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.stubs.StaticTestService;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForIoC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class BackgroundStaticJobWithoutIocRunnerTest {

    private BackgroundStaticJobWithoutIocRunner backgroundStaticJobWithoutIocRunner;

    @BeforeEach
    void setup() {
        backgroundStaticJobWithoutIocRunner = new BackgroundStaticJobWithoutIocRunner();
    }

    @Test
    void doesNotSupportJobIfJobMethodIsNotStatic() {
        Job job = anEnqueuedJob()
                .withJobDetails((IocJobLambda<TestService>) (ts -> ts.doWorkThatFails()))
                .build();

        assertThat(backgroundStaticJobWithoutIocRunner.supports(job)).isFalse();
    }

    @Test
    void doesNotSupportJobIfJobHasStaticField() {
        Job job = anEnqueuedJob()
                .withJobDetails(systemOutPrintLnJobDetails("This is a test"))
                .build();

        assertThat(backgroundStaticJobWithoutIocRunner.supports(job)).isFalse();
    }

    @Test
    void supportsJobIfJobClassHasPrivateConstructorButStaticJobMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> TestServiceForIoC.doWorkInStaticMethod(UUID.randomUUID()))
                .build();

        assertThat(backgroundStaticJobWithoutIocRunner.supports(job)).isTrue();
    }

    @Test
    void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> StaticTestService.doWorkInStaticMethod(UUID.randomUUID()))
                .build();

        assertThatCode(() -> backgroundStaticJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

    @Test
    void runMethodWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(() -> StaticTestService.doWorkInStaticMethod(UUID.randomUUID(), JobContext.Null))
                .build();

        assertThatCode(() -> backgroundStaticJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

}