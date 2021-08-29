package org.jobrunr.stubs;

import org.jobrunr.jobs.lambdas.JobRequest;

public class TestJobRequest implements JobRequest {

    private final String input;

    protected TestJobRequest() {
        this(null);
    }

    public TestJobRequest(String input) {
        this.input = input;
    }

    @Override
    public Class<TestJobRequestHandler> getJobRequestHandler() {
        return TestJobRequestHandler.class;
    }

    public String getInput() {
        return input;
    }
}
