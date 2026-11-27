package org.jobrunr.jobs.details;

public class JobDetailsGeneratorFactory {

    private JobDetailsGeneratorFactory() {
    }

    public static JobDetailsGenerator createJobDetailsGenerator() {
        return new JobDetailsClassFileApiGenerator();
    }
}
