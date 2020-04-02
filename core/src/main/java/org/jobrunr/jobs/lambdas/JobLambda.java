package org.jobrunr.jobs.lambdas;

@FunctionalInterface
public interface JobLambda extends Runnable, JobWithoutIoc {
    // marker interface to make it serializable
}
