package org.jobrunr.server.tasks.zookeeper;

import ch.qos.logback.Logback;
import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.JobRunrAssertions;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.carbonaware.CarbonAware;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareApiWireMockExtension;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.ZonedDateTime;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.server.carbonaware.CarbonAwareApiWireMockExtension.buildForecastSlots;
import static org.jobrunr.server.carbonaware.CarbonAwareApiWireMockExtension.generateCarbonIntensityForecastUsing;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class ProcessRecurringJobsTaskIntegrationTest {

    @RegisterExtension
    static CarbonAwareApiWireMockExtension carbonAwareWiremock = new CarbonAwareApiWireMockExtension();

    private InMemoryStorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private ListAppender<ILoggingEvent> loggerAppender;

    @BeforeEach
    void setUpTests() {
        carbonAwareWiremock.mockResponseWhenRequestingAreaCode("BE", getIntensityForecastFakeDataOptimalAtEarliestTime());
        this.storageProvider = new InMemoryStorageProvider();

        this.loggerAppender = LoggerAssert.initForLogger((Logger) LoggerFactory.getLogger(ProcessRecurringJobsTask.class));

        configureJobRunr();
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
        stopJobRunrBackgroundJobServer();
    }

    private void configureJobRunr() {
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration()
                        .andPollInterval(ofMillis(200))
                        .andCarbonAwareJobProcessingConfiguration(carbonAwareWiremock.getCarbonAwareJobProcessingConfigurationForAreaCode("BE")))
                .initialize();
        this.backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    private void startJobRunrBackgroundJobServer() {
        backgroundJobServer.start();
    }

    private void stopJobRunrBackgroundJobServer() {
        backgroundJobServer.stop();
    }

    @Test
    void carbonAwareAwaitingJobCreatedAndScheduledBeforeIdealTimeNotRecreatedWhenServerReboots() {
        BackgroundJob.scheduleRecurrently("recurring", CarbonAware.using(Duration.ofHours(1), Duration.ofHours(1), Duration.ZERO), () -> System.out.println("carbon job"));
        await().atMost(ofSeconds(5)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        var job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);

        var awaitingScheduled = job.getLastJobStateOfType(CarbonAwareAwaitingState.class).get().getScheduledAt();
        var actuallyScheduled = job.getLastJobStateOfType(ScheduledState.class).get().getScheduledAt();
        assertThat(actuallyScheduled).isBefore(awaitingScheduled);

        // The server rebooted: the ProcessRecurringJobsTask cache is cleared
        stopJobRunrBackgroundJobServer();

        try (var ignored = Logback.temporarilyChangeLogLevel(ProcessRecurringJobsTask.class, Level.TRACE)) {
            startJobRunrBackgroundJobServer();
            await().atMost(ofSeconds(5)).untilAsserted(() -> JobRunrAssertions.assertThat(this.loggerAppender).hasTraceMessageContaining("resulted in 0 scheduled jobs"));

            assertThat(storageProvider.countJobs(SCHEDULED)).isEqualTo(0);
            assertThat(storageProvider.countJobs(ENQUEUED)).isEqualTo(0);
            assertThat(storageProvider.countJobs(PROCESSING)).isEqualTo(0);
            assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(1);
        }
    }

    private CarbonIntensityForecast getIntensityForecastFakeDataOptimalAtEarliestTime() {
        return getIntensityForecastFakeDataOptimalAtIndex(0);
    }

    private CarbonIntensityForecast getIntensityForecastFakeDataOptimalAtIndex(int optimalIndex) {
        ZonedDateTime startTime = ZonedDateTime.now().truncatedTo(SECONDS);
        ZonedDateTime forecastUntil = startTime.plusHours(3);
        var forecast = buildForecastSlots(startTime, forecastUntil, SECONDS, i -> i == optimalIndex ? 0 : i);
        return generateCarbonIntensityForecastUsing(startTime.truncatedTo(DAYS).plusDays(1).withHour(18).withMinute(30), forecast);
    }
}
