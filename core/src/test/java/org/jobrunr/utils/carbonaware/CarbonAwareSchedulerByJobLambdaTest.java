package org.jobrunr.utils.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.junit.jupiter.api.*;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.Duration.ofMillis;
import static org.assertj.core.api.BDDAssertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public class CarbonAwareSchedulerByJobLambdaTest {
    private TestService testService;
    private static StorageProvider storageProvider;
    private static LogAllStateChangesFilter logAllStateChangesFilter;

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(10000);
        wireMockServer.start();
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        storageProvider = new InMemoryStorageProvider();
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/carbon-intensity/v1/day-ahead-energy-prices?area=DE"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(CarbonApiMockResponses.GERMANY_2024_03_14)));
        JobRunr.configure()
                .withJobFilter(logAllStateChangesFilter)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .useCarbonAwareScheduling("DE")
                .initialize();
    }

    @BeforeEach
    void setUpTests() {
        testService = new TestService();
        testService.reset();
        wireMockServer.resetAll();
    }
    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineNow_shouldScheduleNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().minus(3, ChronoUnit.HOURS), Instant.now().plus(1, ChronoUnit.SECONDS),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }


    @Test
    public void testScheduleCarbonAwareJob_withDeadline10Minutes_shouldScheduleNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().minus(3, ChronoUnit.HOURS), Instant.now().plus(10, ChronoUnit.MINUTES),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_shouldScheduleNow() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2026-10-10T08:00:00Z")) {
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withExpiredDeadline_shouldScheduleImmediately() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-14T11:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-15T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            Job job = storageProvider.getJobById(jobId);
            assertThat(job).hasStates(AWAITING, SCHEDULED);
            assertThat(job).hasUpdatedAtCloseTo(Instant.parse("2024-03-14T08:00:00Z"), within(1, ChronoUnit.SECONDS));
            ScheduledState scheduledState = job.getJobState();
            assertThat(scheduledState.getScheduledAt()).isEqualTo(Instant.parse("2024-03-14T12:00:00Z"));
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline2Days_and12HoursData_shouldScheduleAtIdealMoment() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-16T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
            ScheduledState scheduledState = storageProvider.getJobById(jobId).getJobState();
            assertThat(scheduledState.getScheduledAt()).isEqualTo(Instant.parse("2024-03-14T12:00:00Z"));
        }
    }
}
