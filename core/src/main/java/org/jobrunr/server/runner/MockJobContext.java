package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobContext;

/**
 * Class that can be used in tests to set the {@link JobContext} for a {@link org.jobrunr.jobs.lambdas.JobRequestHandler}.
 */
public class MockJobContext {

    private MockJobContext() {}

    public static void setUpJobContextForJob(Job job) {
        MockJobContext.setJobContext(new RunnerJobContext(job));
    }

    public static void setUpJobContext(JobContext jobContext) {
        ThreadLocalJobContext.setJobContext(jobContext);
    }

    /**
     * @param jobContext the JobContext to setup
     * @deprecated use {@link MockJobContext#setUpJobContext(JobContext)}
     */
    @Deprecated
    public static void setJobContext(JobContext jobContext) {
        ThreadLocalJobContext.setJobContext(jobContext);
    }
}
