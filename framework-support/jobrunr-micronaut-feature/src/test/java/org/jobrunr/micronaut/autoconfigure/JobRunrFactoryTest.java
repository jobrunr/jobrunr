package org.jobrunr.micronaut.autoconfigure;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.carbonaware.CarbonAwareConfigurationAssert;
import org.jobrunr.utils.carbonaware.CarbonAwareConfigurationReader;
import org.jobrunr.utils.carbonaware.CarbonAwareJobManager;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Duration;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.micronaut.MicronautAssertions.assertThat;

@MicronautTest(rebuildContext = true)
@TestMethodOrder(MethodOrderer.MethodName.class)
class JobRunrFactoryTest {

    @Inject
    ApplicationContext context;

    @Test
    void aFirstTestThatReloadsTheContextToMakeFlakyTestWork() {
        assertThat(context).hasSingleBean(StorageProvider.class);
    }

    @Test
    @Property(name = "jobrunr.job-scheduler.enabled", value = "true")
    void jobSchedulerEnabledAutoConfiguration() {
        assertThat(context)
                .hasSingleBean(StorageProvider.class)
                .hasSingleBean(JobScheduler.class)
                .hasSingleBean(JobRequestScheduler.class);
    }

    @Test
    @Property(name = "jobrunr.job-scheduler.enabled", value = "false")
    void jobSchedulerDisabledAutoConfiguration() {
        assertThat(context)
                .hasSingleBean(StorageProvider.class)
                .doesNotHaveBean(JobScheduler.class);
    }

    @Test
    @Property(name = "jobrunr.jobs.carbon-aware.area", value = "PL")
    @Property(name = "jobrunr.jobs.carbon-aware.api-client-connect-timeout-ms", value = "500")
    @Property(name = "jobrunr.jobs.carbon-aware.api-client-read-timeout-ms", value = "1000")
    void testCarbonAwareManagerConfiguration() {
        assertThat(context).hasSingleBean(CarbonAwareJobManager.class);
        CarbonAwareJobManager carbonAwareJobManager = context.getBean(CarbonAwareJobManager.class);
        CarbonAwareConfigurationReader carbonAwareConfiguration = carbonAwareJobManager.getCarbonAwareConfiguration();

        CarbonAwareConfigurationAssert.assertThat(carbonAwareConfiguration)
                .hasArea("PL")
                .hasApiClientConnectTimeout(Duration.ofMillis(500))
                .hasApiClientReadTimeout(Duration.ofMillis(1000));
    }

    @Test
    @Property(name = "jobrunr.dashboard.enabled", value = "true")
    void dashboardAutoConfiguration() {
        assertThat(context)
                .hasSingleBean(JobRunrDashboardWebServer.class)
                .doesNotHaveBean(BackgroundJobServer.class);
    }

    @Test
    @Property(name = "jobrunr.dashboard.enabled", value = "true")
    void dashboardAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageDefaultTrue() {
        JobRunrDashboardWebServer dashboardWebServer = context.getBean(JobRunrDashboardWebServer.class);
        assertThat(dashboardWebServer)
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", true);
    }

    @Test
    @Property(name = "jobrunr.dashboard.enabled", value = "true")
    @Property(name = "jobrunr.miscellaneous.allow-anonymous-data-usage", value = "false")
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllowAnonymousDataUsageFalse() {
        JobRunrDashboardWebServer dashboardWebServer = context.getBean(JobRunrDashboardWebServer.class);
        assertThat(dashboardWebServer)
                .hasFieldOrPropertyWithValue("allowAnonymousDataUsage", false);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerAutoConfiguration() {
        assertThat(context)
                .hasSingleBean(BackgroundJobServer.class)
                .doesNotHaveBean(JobRunrDashboardWebServer.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    @Property(name = "jobrunr.background-job-server.name", value = "test")
    void backgroundJobServerAutoConfigurationTakesIntoAccountName() {
        BackgroundJobServer backgroundJobServer = context.getBean(BackgroundJobServer.class);
        assertThat(backgroundJobServer)
                .hasName("test");
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    @Property(name = "jobrunr.jobs.default-number-of-retries", value = "3")
    void backgroundJobServerAutoConfigurationTakesIntoAccountDefaultNumberOfRetries() {
        BackgroundJobServer backgroundJobServer = context.getBean(BackgroundJobServer.class);
        assertThat(backgroundJobServer)
                .hasRetryFilter(3);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    @Property(name = "jobrunr.background-job-server.name", value = "test")
    @Property(name = "jobrunr.background-job-server.worker-count", value = "4")
    @Property(name = "jobrunr.background-job-server.scheduled-jobs-request-size", value = "1")
    @Property(name = "jobrunr.background-job-server.orphaned-jobs-request-size", value = "2")
    @Property(name = "jobrunr.background-job-server.succeeded-jobs-request-size", value = "3")
    @Property(name = "jobrunr.background-job-server.interrupt-jobs-await-duration-on-stop", value = "PT20S")
    void backgroundJobServerAutoConfigurationTakesIntoAccountAllJobsRequestSizes() {
        BackgroundJobServerConfiguration backgroundJobServerConfiguration = context.getBean(BackgroundJobServerConfiguration.class);

        assertThat(backgroundJobServerConfiguration)
                .hasName("test")
                .hasWorkerCount(4)
                .hasScheduledJobRequestSize(1)
                .hasOrphanedJobRequestSize(2)
                .hasSucceededJobRequestSize(3)
                .hasInterruptJobsAwaitDurationOnStopBackgroundJobServer(Duration.ofSeconds(20));
    }
}
