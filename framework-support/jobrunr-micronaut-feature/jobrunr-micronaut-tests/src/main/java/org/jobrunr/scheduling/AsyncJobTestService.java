package org.jobrunr.scheduling;

import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

@Singleton
@AsyncJob
public class AsyncJobTestService {

    @Job(name = "my async job")
    public void runAsyncJob() {
        System.out.println("async job example");
    }

    @Job(name = "my async job with nested async jobs from same service")
    public void runAsyncJobThatCallsAnAsyncJobFromSameService() {
        this.runAsyncJob();
    }

    public int runNonAsyncJob() {
        return 2;
    }
}
