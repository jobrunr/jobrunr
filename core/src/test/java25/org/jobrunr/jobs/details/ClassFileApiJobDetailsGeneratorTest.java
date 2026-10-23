package org.jobrunr.jobs.details;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassFileApiJobDetailsGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new ClassFileApiJobDetailsGenerator();
    }

    @Test
    void factorySelectsTheClassFileApiBackendWhenTheJvmSupportsIt() {
        assertThat(JobDetailsGeneratorFactory.createDefault()).isInstanceOf(ClassFileApiJobDetailsGenerator.class);
    }
}
