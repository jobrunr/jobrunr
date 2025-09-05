package org.jobrunr.micronaut.scheduling;

//@Singleton
//@Requires(property = "test.asyncjob.wrongtype.enabled", value = "true")
//@AsyncJob
public class AsyncJobTestServiceWithWrongReturnType {

    //@Job
    public int testMethodAsAsyncJobWithSomeReturnType() {
        System.out.println("async job example");
        return 2;
    }

}
