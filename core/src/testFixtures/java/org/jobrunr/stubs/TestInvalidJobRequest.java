package org.jobrunr.stubs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.stubs.TestJobContextJobRequest.TestJobContextJobRequestHandler;

public class TestInvalidJobRequest implements JobRequest {

    @Override
    public Class<TestJobContextJobRequestHandler> getJobRequestHandler() {
        return TestJobContextJobRequestHandler.class;
    }
}
