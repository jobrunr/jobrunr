package org.jobrunr.jobs.details;

class JobDetailsAsmGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new JobDetailsAsmGenerator();
    }
}
