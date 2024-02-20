package org.jobrunr.tests.e2e.services;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.BackgroundJob;

import java.util.UUID;

public class TestService {

    public void doWork() {
        System.out.println("This is a test service");
    }

    public void doWork(Work work) {
        System.out.println("This is a test service " + work.getSomeId() + "; " + work.getSomeString() + "; " + work.getSomeInt() + "; " + work.getSomeLong());
    }

    public void doWorkWithMultipleParameters(JobContext jobContext, long someLongValue, Work work) {
        System.out.println("This is a test service with an extra long value: " + someLongValue + "; Work: " + work.getSomeId() + "; " + work.getSomeString() + "; " + work.getSomeInt() + "; " + work.getSomeLong());
    }

    public void run() {
        Work work = new Work(123, "some string", 456L, UUID.randomUUID());
        long someLongValue = 789L;

        BackgroundJob.enqueue(() -> this.doWorkWithMultipleParameters(JobContext.Null, someLongValue, work));
    }

}
