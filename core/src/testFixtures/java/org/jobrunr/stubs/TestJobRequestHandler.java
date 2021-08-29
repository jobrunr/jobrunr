package org.jobrunr.stubs;


import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobRequestHandler implements JobRequestHandler<TestJobRequest> {

    private JobContext jobContext;

    @Override
    public void run(TestJobRequest jobRequest) {
        System.out.println("Running simple job request in background: " + jobRequest.getInput());
        jobContext.getMetadata().put("test", "test");
    }
}
