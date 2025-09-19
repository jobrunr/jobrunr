package org.jobrunr.server.runner;

import org.jobrunr.jobs.context.JobContext;

public class ThreadLocalJobContext {

    private ThreadLocalJobContext() {
    }

    private static final ThreadLocal<JobContext> JOB_CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    static void setJobContext(JobContext jobContext) {
        JOB_CONTEXT_THREAD_LOCAL.set(jobContext);
    }

    static void clear() {
        JOB_CONTEXT_THREAD_LOCAL.remove();
    }

    public static JobContext getJobContext() {
        return JOB_CONTEXT_THREAD_LOCAL.get();
    }
}
