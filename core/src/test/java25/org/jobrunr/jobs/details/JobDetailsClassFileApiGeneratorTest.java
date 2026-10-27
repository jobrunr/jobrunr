package org.jobrunr.jobs.details;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JobDetailsClassFileApiGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new JobDetailsClassFileApiGenerator();
    }

    @Test
    void factorySelectsTheClassFileApiBackendWhenTheJvmSupportsIt() {
        assertThat(JobDetailsGeneratorFactory.createDefault()).isInstanceOf(JobDetailsClassFileApiGenerator.class);
    }
}
