package org.jobrunr.jobs.lambdas;

/**
 * Classes implementing this interface will handle actual {@link JobRequest}.
 *
 * @param <T> A JobRequest implementation that can have extra fields and methods to be used by the {@code JobRequestHandler}.
 */
public interface JobRequestHandler<T extends JobRequest> {

    void run(T jobRequest);

}
