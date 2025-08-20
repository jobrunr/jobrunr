package org.jobrunr.quarkus.scheduling;

import jakarta.inject.Singleton;
import org.jobrunr.jobs.annotations.AsyncJob;
import org.jobrunr.jobs.annotations.Job;

@Singleton
@AsyncJob
public class AsyncJobTestService {

    @Job
    public void runSomeJob() {
        System.out.println("async job example");
    }

    public int classicMethod() {
        return 2;
    }
}
