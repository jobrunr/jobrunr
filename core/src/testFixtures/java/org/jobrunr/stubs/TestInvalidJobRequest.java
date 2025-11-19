package org.jobrunr.stubs;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.stubs.TestJobRequest.TestJobRequestHandler;

public class TestInvalidJobRequest implements JobRequest {

    @Override
    public Class<TestJobRequestHandler> getJobRequestHandler() {
        return TestJobRequestHandler.class;
    }
}
