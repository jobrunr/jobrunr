package org.jobrunr.jobs.details;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobDetailsClassFileApiGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new JobDetailsClassFileApiGenerator();
    }

    @Test
    void factorySelectsTheClassFileApiBackendWhenTheJvmSupportsIt() {
        assertThat(JobDetailsGeneratorFactory.createJobDetailsGenerator()).isInstanceOf(JobDetailsClassFileApiGenerator.class);
    }

    @Test
    void asmIsNotOnTheClassPath() {
        assertThatThrownBy(() -> Class.forName("org.objectweb.asm.Opcodes", false, getClass().getClassLoader()))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
