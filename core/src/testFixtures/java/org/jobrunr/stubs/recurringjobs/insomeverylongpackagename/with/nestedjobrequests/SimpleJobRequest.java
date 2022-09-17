package org.jobrunr.stubs.recurringjobs.insomeverylongpackagename.with.nestedjobrequests;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class SimpleJobRequest implements JobRequest {
    @Override
    public Class<? extends JobRequestHandler> getJobRequestHandler() {
        return SimpleJobRequestHandler.class;
    }

    public static class SimpleJobRequestHandler implements JobRequestHandler<SimpleJobRequest> {

        @Override
        public void run(SimpleJobRequest jobRequest) throws Exception {
            System.out.println("Received a job jobRequest");
        }
    }
}
