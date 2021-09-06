package org.jobrunr.jobs.lambdas;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.ThreadLocalJobContext;

/**
 * Classes implementing this interface will handle the actual {@link JobRequest}.
 *
 * @param <T> A JobRequest implementation that can have extra fields and methods to be used by the {@code JobRequestHandler}.
 */
public interface JobRequestHandler<T extends JobRequest> {

    void run(T jobRequest) throws Exception;

    /**
     * Gives access to the JobContext for the current job in a thread-safe manner. It will be available only during the {@link #run(JobRequest)} method.
     * @return the {@link JobContext} for the current Job
     */
    default JobContext jobContext() {
        return ThreadLocalJobContext.getJobContext();
    }
}
