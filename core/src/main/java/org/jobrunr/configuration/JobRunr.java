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
 *
 * Both the backgroundJobServer and the dashboard fluent API allow to be enabled or disabled using ENV variables which
 * is handy in docker containers.
 * <h5>An example:</h5>
 * <pre>
 *     boolean isBackgroundJobServerEnabled = true; // or get it via ENV variables
 *     boolean isDashboardEnabled = true; // or get it via ENV variables
 *     JobRunr.configure()
 *                 .useJobStorageProvider(jobStorageProvider)
 *                 .useJobActivator(jobActivator)
 *                 .useDefaultBackgroundJobServerIf(isBackgroundJobServerEnabled)
 *                 .useDashboardIf(isDashboardEnabled)
 *                 .useJmxExtensions()
 *                 .initialize();
 * </pre>
 */
public class JobRunr {

    private static JobRunrConfiguration jobRunrConfiguration;

    private JobRunr() {
    }

    public static JobRunrConfiguration configure() {
        jobRunrConfiguration = new JobRunrConfiguration();
        Runtime.getRuntime().addShutdownHook(new Thread(JobRunr::destroy, "extShutdownHook"));
        return jobRunrConfiguration;
    }

    public static JobRunrConfiguration destroy() {
        if (jobRunrConfiguration != null) {
            if (jobRunrConfiguration.backgroundJobServer != null) jobRunrConfiguration.backgroundJobServer.stop();
            if (jobRunrConfiguration.dashboardWebServer != null) jobRunrConfiguration.dashboardWebServer.stop();
            if (jobRunrConfiguration.storageProvider != null) jobRunrConfiguration.storageProvider.close();
        }
        return jobRunrConfiguration;
    }
}
