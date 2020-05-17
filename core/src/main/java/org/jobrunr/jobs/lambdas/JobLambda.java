package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface JobLambda extends JobRunrJob {

    void run() throws Exception;
}
