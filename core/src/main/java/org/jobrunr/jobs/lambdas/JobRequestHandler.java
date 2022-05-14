package org.jobrunr.jobs.lambdas;

import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.server.runner.ThreadLocalJobContext;

/**
 * Classes implementing this interface will handle the actual {@link JobRequest}.
 *
 * @param <T> A JobRequest implementation that can have extra fields and methods to be used by the {@code JobRequestHandler}.
 */
public interface JobRequestHandler<T extends JobRequest> {

    /**
     * The actual job processing to perform.
     * @param jobRequest the {@link JobRequest} to be processed
     * @throws Exception if an error occurs during the processing, JobRunr will automatically retry the job.
     */
    void run(T jobRequest) throws Exception;

    /**
     * Gives access to the JobContext for the current job in a thread-safe manner. It will be available only during the {@link #run(JobRequest)} method.
     *
     * To set it in tests, use the {@link org.jobrunr.server.runner.MockJobContext} class.
     *
     * @return the {@link JobContext} for the current Job
     */
    default JobContext jobContext() {
        return ThreadLocalJobContext.getJobContext();
    }
}
