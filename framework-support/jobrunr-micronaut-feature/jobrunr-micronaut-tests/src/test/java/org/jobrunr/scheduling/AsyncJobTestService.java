package org.jobrunr.scheduling;

import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

@Singleton
@AsyncJob
public class AsyncJobTestService {

    @Job
    public void createSomeJob() {
        System.out.println("async job example");
    }
}
