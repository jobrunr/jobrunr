package org.jobrunr.stubs;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobContextAware;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobRequest implements JobRequest {

    private final String input;
    private final boolean mustFail;
    private final Integer retryCount;
    private final String jobName;

    protected TestJobRequest() {
        this(null);
    }

    public TestJobRequest(String input) {
        this(input, null);
    }

    public TestJobRequest(String input, String jobName) {
        this.input = input;
        this.jobName = jobName;
        this.mustFail = false;
        this.retryCount = null;
    }

    public TestJobRequest(String input, boolean mustFail, int retryCount) {
        this.input = input;
        this.jobName = null;
        this.mustFail = mustFail;
        this.retryCount = retryCount;
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

    public static class TestJobRequestHandler implements JobRequestHandler<TestJobRequest>, JobContextAware {

        private JobContext jobContext;

        @Override
        @Job(name = "Some neat Job Display Name", retries = 2)
        public void run(TestJobRequest jobRequest) {
            if (jobRequest.mustFail()) throw new IllegalArgumentException("it must fail");
            System.out.println("Running simple job request in background: " + jobRequest.getInput());
            jobContext.getMetadata().put("test", "test");
        }

        @Override
        public void setJobContext(JobContext jobContext) {
            this.jobContext = jobContext;
        }
    }
}
