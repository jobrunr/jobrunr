package org.jobrunr.configuration;

/**
 * This class provides the entry point for the JobRunr configuration. This is needed when you want to use the static methods
 * on {@link org.jobrunr.scheduling.BackgroundJob} to enqueue and schedule jobs. It also allows to startup the Dashboard which
 * will be available on port 8000.
 *
 * <h5>An example:</h5>
 * <pre>
 *      JobRunr.configure()
 *                 .useJobStorageProvider(jobStorageProvider)
 *                 .useJobActivator(jobActivator)
 *                 .useDefaultBackgroundJobServer()
 *                 .useDashboard()
 *                 .initialize();
 * </pre>
 */
public class JobRunr {

    private JobRunr() {
    }

    public static JobRunrConfiguration configure() {
        return new JobRunrConfiguration();
    }
}
