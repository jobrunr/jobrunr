package org.jobrunr.configuration;

import org.jobrunr.server.BackgroundJobServer;

/**
 * This class provides the entry point for the JobRunr configuration. This is needed when you want to use the static methods
 * on {@link org.jobrunr.scheduling.BackgroundJob} to enqueue and schedule jobs. It also allows starting the Dashboard, which
 * will be available on port 8000.
 *
 * <h5>An example:</h5>
 * <pre>
 *      JobRunr.configure()
 *                 .useJobActivator(jobActivator)
 *                 .useJobStorageProvider(jobStorageProvider)
 *                 .useBackgroundJobServer()
 *                 .useJmxExtensions()
 *                 .useDashboard()
 *                 .initialize();
 * </pre>
 * <p>
 * Both the backgroundJobServer and the dashboard fluent API allow to be enabled or disabled using ENV variables which
 * is handy in docker containers.
 * <h5>An example:</h5>
 * <pre>
 *     boolean isBackgroundJobServerEnabled = true; // or get it via ENV variables
 *     boolean isDashboardEnabled = true; // or get it via ENV variables
 *     JobRunr.configure()
 *                 .useJobStorageProvider(jobStorageProvider)
 *                 .useJobActivator(jobActivator)
 *                 .useBackgroundJobServerIf(isBackgroundJobServerEnabled)
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
            if (jobRunrConfiguration.microMeterIntegration != null) jobRunrConfiguration.microMeterIntegration.close();
        }
        return jobRunrConfiguration;
    }

    public static BackgroundJobServer getBackgroundJobServer() {
        if (jobRunrConfiguration == null)
            throw new IllegalStateException("You don't seem to use the Fluent API. This method is only available if you are using the Fluent API to configure JobRunr");
        if (jobRunrConfiguration.backgroundJobServer == null)
            throw new IllegalStateException("The background job server is not configured. Are you perhaps only running the JobScheduler or the Dashboard on this server instance?");
        return jobRunrConfiguration.backgroundJobServer;
    }
}
