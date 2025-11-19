package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;

/**
 * Class that can be used in tests to set the {@link JobContext} for a {@link org.jobrunr.jobs.lambdas.JobRequestHandler}.
 */
public class MockJobContext {

    private MockJobContext() {
    }

    public static void setUpJobContextForJob(Job job) {
        MockJobContext.setUpJobContext(new RunnerJobContext(job));
    }

    public static void setUpJobContext(JobContext jobContext) {
        ThreadLocalJobContext.setJobContext(jobContext);
    }
}
