package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobRequest implements JobRequest {

    private final String input;
    private final boolean mustFail;
    private final long sleepAfterFinished;

    protected TestJobRequest() {
        this(null);
    }

    public TestJobRequest(String input) {
        this(input, false, 0L);
    }

    public TestJobRequest(String input, long sleepAfterFinished) {
        this(input, false, sleepAfterFinished);
    }

    public TestJobRequest(String input, boolean mustFail) {
        this(input, mustFail, 0L);
    }

    public TestJobRequest(String input, boolean mustFail, long sleepAfterFinished) {
        this.input = input;
        this.mustFail = mustFail;
        this.sleepAfterFinished = sleepAfterFinished;
    }

    @Override
    public Class<TestJobRequestHandler> getJobRequestHandler() {
        return TestJobRequestHandler.class;
    }

    public String getInput() {
        return input;
    }

    public boolean mustFail() {
        return mustFail;
    }

    public static class TestJobRequestHandler implements JobRequestHandler<TestJobRequest> {

        @Override
        @Job(name = "Some neat Job Display Name", retries = 1)
        public void run(TestJobRequest jobRequest) throws InterruptedException {
            if (jobRequest.mustFail()) throw new IllegalArgumentException("it must fail");
            System.out.println("Running simple job request in background: " + jobRequest.getInput());
            jobContext().saveMetadata("test", "test");
            if(jobRequest.sleepAfterFinished > 0L) {
                Thread.sleep(jobRequest.sleepAfterFinished * 1000);
            }
        }
    }
}
