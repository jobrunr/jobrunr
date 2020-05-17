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
    public void setup() {
        backgroundJobWithoutIocRunner = new BackgroundJobWithoutIocRunner();
    }

    @Test
    public void supportsJobIfJobClassHasDefaultConstructor() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThat(backgroundJobWithoutIocRunner.supports(job)).isTrue();
    }

    @Test
    public void doesNotSupportJobIfClassHasNoDefaultConstructor() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails().withClassName(TestServiceForIoC.class))
                .build();

        assertThat(backgroundJobWithoutIocRunner.supports(job)).isFalse();
    }

    @Test
    public void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThatCode(() -> backgroundJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

    @Test
    public void runMethodWithJobContext() {
        Job job = anEnqueuedJob()
                .withId()
                .withJobDetails(defaultJobDetails()
                        .withJobParameter(JobContext))
                .build();

        assertThatCode(() -> backgroundJobWithoutIocRunner.run(job)).doesNotThrowAnyException();
    }

}