package org.jobrunr.tests.e2e;

import org.jobrunr.scheduling.carbonaware.CarbonAware;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.tests.server.AbstractSimpleBackgroundJobServer;
import org.jobrunr.tests.server.SimpleCarbonAwareBackgroundJobServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

import static java.time.Duration.ofSeconds;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonIntensityApiStubServer.CARBON_LOW_INTENSITY;
import static org.jobrunr.tests.e2e.PlaywrightAssertions.assertThat;

public class DashboardCarbonIntensityChartE2ETest extends AbstractPlaywrightE2ETest {

    private static AbstractSimpleBackgroundJobServer server;
    private static StorageProvider storageProvider;

    @BeforeAll
    static void beforeAll() {
        storageProvider = new InMemoryStorageProvider();
        server = new SimpleCarbonAwareBackgroundJobServer()
                .andRecurringJobs(aDefaultRecurringJob()
                        .withId("carbon-aware-every-3h")
                        .withName("carbon aware job every 3h")
                        .withIntervalExpression(CarbonAware.interval(Duration.ofHours(3), Duration.ofHours(1)))
                        .build())
                .andBestIntensityMomentToday(optimalTime().getHour()) // since our job is PT3H [PT1H/PT1H]
                .withStorageProvider(storageProvider);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void carbonIntensityChartIsRenderedInScheduledJobDetail() {
        // Our sever is not paused; wait for the recurring job & carbon aware tasks.
        await().atMost(ofSeconds(5)).until(() -> storageProvider.countJobs(SCHEDULED) == 1);

        page.navigate("http://localhost:8000/dashboard/jobs?state=SCHEDULED");
        page.waitForLoadState();

        scheduledMenuBtn().click();

        assertThat(jobTableRows()).hasCount(1);
        jobTableRowsClickOnFirstJob();
        title("Job Pending - Ahead of time by recurring job 'carbon aware job every 3h'").click();

        assertThat(carbonIntensityChart()).isVisible();
        var optimalTimeInUI = carbonIntensityChart().locator(".carbon-intensity-chart-block-best").getAttribute("title");

        assertThat(optimalTimeInUI).isEqualTo(optimalTime().format(ofPattern("HH:00")) + ": Optimal execution window — Intensity: " + CARBON_LOW_INTENSITY);
    }

    private static LocalTime optimalTime() {
        // If it's 12:04, [12:00-13:00] will not be considered so add another hour.
        return LocalTime.now(ZoneId.systemDefault()).truncatedTo(HOURS).plus(3, HOURS);
    }

}

