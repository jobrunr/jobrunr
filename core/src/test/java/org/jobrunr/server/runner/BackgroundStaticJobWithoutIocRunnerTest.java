package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class BackgroundStaticJobWithoutIocRunnerTest {

    private BackgroundStaticJobWithoutIocRunner backgroundStaticJobWithoutIocRunner;

    @BeforeEach
    public void setup() {
        backgroundStaticJobWithoutIocRunner = new BackgroundStaticJobWithoutIocRunner();
    }

    @Test
    public void supportsJobIfJobIsStaticMethodCall() {
        Job job = anEnqueuedJob()
                .withJobDetails(systemOutPrintLnJobDetails("This is a test"))
                .build();

        assertThat(backgroundStaticJobWithoutIocRunner.supports(job)).isTrue();
    }

    @Test
    public void doesNotSupportJobIfJobIsNotAStaticMethodCall() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThat(backgroundStaticJobWithoutIocRunner.supports(job)).isFalse();
    }

    @Test
    public void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(systemOutPrintLnJobDetails("This is a test"))
                .build();

        assertThatCode(() -> backgroundStaticJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

}