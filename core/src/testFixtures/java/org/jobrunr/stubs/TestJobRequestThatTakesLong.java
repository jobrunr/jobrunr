package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobRequestThatTakesLong implements JobRequest {

    private final String input;
    private final boolean mustFail;
    private final int nbrOfSeconds;

    protected TestJobRequestThatTakesLong() {
        this(null);
    }

    public TestJobRequestThatTakesLong(String input) {
        this(input, false, 0);
    }
    public TestJobRequestThatTakesLong(String input, int nbrOfSeconds) {
        this(input, false, nbrOfSeconds);
    }

    public TestJobRequestThatTakesLong(String input, boolean mustFail, int nbrOfSeconds) {
        this.input = input;
        this.mustFail = mustFail;
        this.nbrOfSeconds = nbrOfSeconds;
    }

    @Override
    public Class<TestJobRequestThatTakesLongHandler> getJobRequestHandler() {
        return TestJobRequestThatTakesLongHandler.class;
    }

    public String getInput() {
        return input;
    }

    public boolean mustFail() {
        return mustFail;
    }

    public static class TestJobRequestThatTakesLongHandler implements JobRequestHandler<TestJobRequestThatTakesLong> {

        @Override
        @Job(name = "Some neat Job Display Name", retries = 1)
        public void run(TestJobRequestThatTakesLong jobRequest) throws InterruptedException {
            if (jobRequest.mustFail()) throw new IllegalArgumentException("it must fail");
            if(jobRequest.nbrOfSeconds > 0) {
                Thread.sleep(jobRequest.nbrOfSeconds * 1000);
            }
            System.out.println("Running simple job request in background: " + jobRequest.getInput());
            jobContext().saveMetadata("test", "test");
        }
    }
}
