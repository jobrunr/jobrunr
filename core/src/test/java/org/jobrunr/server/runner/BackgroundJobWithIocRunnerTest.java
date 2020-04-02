package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobActivator;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobDetailsTestBuilder.defaultJobDetails;
import static org.jobrunr.jobs.JobParameter.JobContext;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class BackgroundJobWithIocRunnerTest {

    private BackgroundJobWithIocRunner backgroundIoCJobWithIocRunner;

    @BeforeEach
    public void setup() {
        backgroundIoCJobWithIocRunner = new BackgroundJobWithIocRunner(new JobActivator() {
            @Override
            public <T> T activateJob(Class<T> type) {
                return (T) new TestService();
            }
        });
    }

    @Test
    public void runSimpleMethod() {
        Job job = anEnqueuedJob()
                .withJobDetails(defaultJobDetails())
                .build();

        assertThatCode(() -> backgroundIoCJobWithIocRunner.run(job)).doesNotThrowAnyException();
    }

    @Test
    public void runMethodWithJobContext() {
        Job job = anEnqueuedJob()
                .withId()
                .withJobDetails(defaultJobDetails()
                        .withJobParameter(JobContext))
                .build();

        assertThatCode(() -> backgroundIoCJobWithIocRunner.run(job)).doesNotThrowAnyException();
    }

}