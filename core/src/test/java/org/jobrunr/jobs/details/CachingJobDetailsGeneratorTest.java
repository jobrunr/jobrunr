package org.jobrunr.jobs.details;

public class CachingJobDetailsGeneratorTest extends AbstractJobDetailsGeneratorTest {

    @Override
    protected JobDetailsGenerator getJobDetailsGenerator() {
        return new CachingJobDetailsGenerator(new JobDetailsAsmGenerator());
    }
}
