package org.jobrunr.scheduling;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

@Singleton
@Requires(property = "test.asyncjob.wrongtype.enabled", value = "true")
@AsyncJob
public class AsyncJobTestServiceWithWrongReturnType {

    @Job
    public int runAsyncJobWithReturnType() {
        System.out.println("async job example");
        return 2;
    }

}
