package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface JobLambda extends JobWithoutIoc {
    // marker interface to make it serializable
    void run() throws Exception;
}
