package org.jobrunr.server.runner;

import org.jobrunr.JobRunrException;
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

    /**
     * Returns the {@link JobContext} for the currently executing job. This method retrieves the {@link JobContext} associated with the job being processed,
     * allowing access to job metadata, progress reporting, and logging capabilities.
     *
     * @return the {@link JobContext} for the currently executing job
     * @throws JobRunrException if no {@link JobContext} is available
     */
    public static JobContext getJobContext() {
        JobContext jobContext = jobContextThreadLocal.get();
        if (jobContext == null) {
            throw new JobRunrException("No JobContext available. This method can only be called from within a job being executed by a JobRunr worker. Are you perhaps testing the job? Then you should use MockThreadLocalJobContext from JobRunr's test-fixtures to set a mock JobContext.");
        }
        return jobContext;
    }

    public static boolean hasJobContext() {
        return jobContextThreadLocal.get() != null;
    }
}
