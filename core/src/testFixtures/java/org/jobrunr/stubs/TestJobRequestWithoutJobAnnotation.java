package org.jobrunr.stubs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobRequestWithoutJobAnnotation implements JobRequest {

    private final String input;
    private final boolean mustFail;
    private final long sleepAfterFinished;

    protected TestJobRequestWithoutJobAnnotation() {
        this(null);
    }

    public TestJobRequestWithoutJobAnnotation(String input) {
        this(input, false, 0L);
    }

    public TestJobRequestWithoutJobAnnotation(String input, long sleepAfterFinished) {
        this(input, false, sleepAfterFinished);
    }

    public TestJobRequestWithoutJobAnnotation(String input, boolean mustFail) {
        this(input, mustFail, 0L);
    }

    public TestJobRequestWithoutJobAnnotation(String input, boolean mustFail, long sleepAfterFinished) {
        this.input = input;
        this.mustFail = mustFail;
        this.sleepAfterFinished = sleepAfterFinished;
    }

    @Override
    public Class<TestWithoutJobAnnotationJobRequestHandler> getJobRequestHandler() {
        return TestWithoutJobAnnotationJobRequestHandler.class;
    }

    public String getInput() {
        return input;
    }

    public boolean mustFail() {
        return mustFail;
    }

    public static class TestWithoutJobAnnotationJobRequestHandler implements JobRequestHandler<TestJobRequestWithoutJobAnnotation> {

        @Override
        public void run(TestJobRequestWithoutJobAnnotation jobRequest) throws InterruptedException {
            if (jobRequest.mustFail()) throw new IllegalArgumentException("it must fail");
            System.out.println("Running simple job request in background: " + jobRequest.getInput());
            jobContext().saveMetadata("test", "test");
            if(jobRequest.sleepAfterFinished > 0L) {
                Thread.sleep(jobRequest.sleepAfterFinished * 1000);
            }
        }
    }
}
