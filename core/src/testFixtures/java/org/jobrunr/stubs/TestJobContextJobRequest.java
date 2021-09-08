package org.jobrunr.stubs;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

public class TestJobContextJobRequest implements JobRequest {

    @Override
    public Class<TestJobContextJobRequestHandler> getJobRequestHandler() {
        return TestJobContextJobRequestHandler.class;
    }

    public static class TestJobContextJobRequestHandler implements JobRequestHandler<TestJobContextJobRequest> {

        @Override
        public void run(TestJobContextJobRequest jobRequest) throws Exception {
            JobContext jobContext = jobContext();
            for(int i = 0; i < 100; i++) {
                jobContext.saveMetadata("key" + i, jobContext.getJobId());
                Thread.sleep(5);
            }
            jobContext.logger().info("Successfully finished job " + jobContext.getJobId());
        }
    }
}
