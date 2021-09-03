package org.jobrunr.jobs.lambdas;

/**
 * Classes implementing this interface can be used to enqueue a JobRunr Job and will be used as the
 * argument for the actual {@link JobRequestHandler}.
 *
 * <strong>Make sure that your {@code JobRequest} class implementation can be serialized by your chosen Json library.</strong>
 * You will need a default no-arg constructor for deserialization.
 * <p>
 * While processing, JobRunr will lookup the actual {@link JobRequestHandler} in the IoC container or create a
 * new instance using the default constructor. Next, it will call the {@code run} method and pass it JobRequest as argument.
 */
public interface JobRequest extends JobRunrJob {

    Class<? extends JobRequestHandler> getJobRequestHandler();

}
