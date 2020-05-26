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
 *                 .useJmxExtensions()
 *                 .useDashboard()
 *                 .initialize();
 * </pre>
 */
public class JobRunr {

    private static JobRunrConfiguration jobRunrConfiguration;

    private JobRunr() {
    }

    public static JobRunrConfiguration configure() {
        jobRunrConfiguration = new JobRunrConfiguration();
        return jobRunrConfiguration;
    }

    public static JobRunrConfiguration destroy() {
        jobRunrConfiguration.backgroundJobServer.stop();
        jobRunrConfiguration.dashboardWebServer.stop();
        return jobRunrConfiguration;
    }
}
