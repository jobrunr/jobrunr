package org.jobrunr.server.runner;

import org.jobrunr.jobs.context.JobContext;

public class ThreadLocalJobContext {

    private ThreadLocalJobContext() {
    }

    private static final ThreadLocal<JobContext> jobContextThreadLocal = new ThreadLocal<>();

    static void setJobContext(JobContext jobContext) {
        jobContextThreadLocal.set(jobContext);
    }

    static void clear() {
        jobContextThreadLocal.remove();
    }

    public static JobContext getJobContext() {
        return jobContextThreadLocal.get();
    }
}
