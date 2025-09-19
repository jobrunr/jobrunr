package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.stubs.TestServiceForIoC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobParameter.JobContext;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class BackgroundJobWithoutIocRunnerTest {

    private BackgroundJobWithoutIocRunner backgroundJobWithoutIocRunner;

    @BeforeEach
    void setup() {
        backgroundJobWithoutIocRunner = new BackgroundJobWithoutIocRunner();
    }

    @Test
    void supportsJobIfJobClassHasDefaultConstructor() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThat(backgroundJobWithoutIocRunner.supports(job)).isTrue();
    }

    @Test
    void doesNotSupportJobIfClassHasNoDefaultConstructor() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails().withClassName(TestServiceForIoC.class))
                .build();

        assertThat(backgroundJobWithoutIocRunner.supports(job)).isFalse();
    }

    @Test
    void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThatCode(() -> backgroundJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

    @Test
    void runMethodWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails()
                        .withJobParameter(JobContext))
                .build();

        assertThatCode(() -> backgroundJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

}