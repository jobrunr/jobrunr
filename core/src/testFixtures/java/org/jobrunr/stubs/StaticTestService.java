package org.jobrunr.stubs;

import org.jobrunr.jobs.context.JobContext;

import java.util.UUID;

public class StaticTestService {

    private StaticTestService() {
        // private constructor for SonarQube
    }

    public static void doWorkInStaticMethodWithoutParameter() {
        System.out.println("Doing work in static method without UUID.");
    }

    public static void doWorkInStaticMethod(UUID id) {
        System.out.println("Doing work in static method: " + id);
    }

    public static void doWorkInStaticMethod(UUID id, JobContext jobContext) {
        System.out.println("Doing work in static method: " + id + " and job context name " + jobContext.getJobName());
    }
}
