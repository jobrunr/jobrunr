package org.jobrunr.micronaut.autoconfigure;


import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jobrunr.dashboard.JobRunrDashboardWebServer;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

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
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context).hasSingleBean(JobScheduler.class);
        assertThat(context).hasSingleBean(JobRequestScheduler.class);
    }

    @Test
    @Property(name = "jobrunr.job-scheduler.enabled", value = "false")
    void jobSchedulerDisabledAutoConfiguration() {
        assertThat(context).hasSingleBean(StorageProvider.class);
        assertThat(context).doesNotHaveBean(JobScheduler.class);
    }

    @Test
    @Property(name = "jobrunr.dashboard.enabled", value = "true")
    void dashboardAutoConfiguration() {
        assertThat(context).hasSingleBean(JobRunrDashboardWebServer.class);
        assertThat(context).doesNotHaveBean(BackgroundJobServer.class);
    }

    @Test
    @Property(name = "jobrunr.background-job-server.enabled", value = "true")
    void backgroundJobServerAutoConfiguration() {
        assertThat(context).hasSingleBean(BackgroundJobServer.class);
        assertThat(context).doesNotHaveBean(JobRunrDashboardWebServer.class);
    }
}
