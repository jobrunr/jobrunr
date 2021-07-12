package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForIoC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobParameter.JobContext;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class BackgroundJobWithIocRunnerTest {

    private BackgroundJobWithIocRunner backgroundIoCJobWithIocRunner;

    @BeforeEach
    void setup() {
        backgroundIoCJobWithIocRunner = new BackgroundJobWithIocRunner(new SimpleJobActivator(new TestService()));
    }

    @Test
    void supportsJobIfJobClassIsKnownInIoC() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThat(backgroundIoCJobWithIocRunner.supports(job)).isTrue();
    }

    @Test
    void doesNotSupportJobIfNoJobActivatorIsRegistered() {
        backgroundIoCJobWithIocRunner = new BackgroundJobWithIocRunner(null);

        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThat(backgroundIoCJobWithIocRunner.supports(job)).isFalse();
    }

    @Test
    void doesNotSupportJobIfJobClassIsNotKnownInIoC() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails().withClassName(TestServiceForIoC.class))
                .build();

        assertThat(backgroundIoCJobWithIocRunner.supports(job)).isFalse();
    }

    @Test
    void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThatCode(() -> backgroundIoCJobWithIocRunner.run(job)).doesNotThrowAnyException();
    }

    @Test
    void runMethodWithJobContext() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails()
                        .withJobParameter(JobContext))
                .build();

        assertThatCode(() -> backgroundIoCJobWithIocRunner.run(job)).doesNotThrowAnyException();
    }

}