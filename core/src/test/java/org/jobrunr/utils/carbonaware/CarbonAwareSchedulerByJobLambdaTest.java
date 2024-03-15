package org.jobrunr.utils.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobId;
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
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadlineNow_shouldScheduleNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now(),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }


    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadlineNow_shouldScheduleNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(CarbonAware.of(),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadlineOneMinute_shouldScheduleNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plusSeconds(60),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andBestHourIn4Hours_shouldScheduleAtIdealMoment() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plus(1, ChronoUnit.DAYS),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_shouldWaitUntilDeadline() {
        // in this case we wait until the deadline, in case data becomes available in the meantime
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2026-10-10T08:00:00Z")) {
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plus(1, ChronoUnit.DAYS),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withExpiredDeadline_shouldScheduleImmediately() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-14T08:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-15T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment"));
            // NOTE: this test sometimes fails at random when running all tests.
            // org.awaitility.core.ConditionTimeoutException: Condition with org.jobrunr.utils.carbonaware.CarbonAwareSchedulerByJobLambdaTest was not fulfilled within 10 seconds.
            await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadline2Days_and12HoursData_andSaturdayBeforeDeadline_shouldWait() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-16T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    @Order(1)
    public void testScheduleCarbonAwareJob_withDeadline3Days_and12HoursData_andSundayBeforeDeadline_shouldWait() {
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-17T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }
    @Test
    @Order(2)
    public void testScheduleCarbonAwareJob_withSaturdayData_andSundayNotInDeadline_shouldScheduleOnIdealTime() {
        configureJobRunrWithSaturdayData();
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-15T16:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-16T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }

    @Test
    @Order(3)
    public void testScheduleCarbonAwareJob_withSaturdayData_andSundayInDeadline_shouldWait() {
        configureJobRunrWithSaturdayData();
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-15T16:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-17T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    @Order(4)
    public void testScheduleCarbonAwareJob_withSundayData_andSundayInDeadline_shouldScheduleOnIdealTime() {
        configureJobRunrWithSundayData();
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-16T16:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-17T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }


    private void configureJobRunrWithSaturdayData() {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/carbon-intensity/v1/day-ahead-energy-prices?area=DE"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(CarbonApiMockResponses.GERMANY_SATURDAY_2024_03_16)));
        JobRunr.configure()
                .withJobFilter(logAllStateChangesFilter)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .useCarbonAwareScheduling("DE")
                .initialize();
    }

    private void configureJobRunrWithSundayData() {
        wireMockServer.stubFor(WireMock.get(urlEqualTo("/carbon-intensity/v1/day-ahead-energy-prices?area=DE"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(CarbonApiMockResponses.GERMANY_SUNDAY_2024_03_17)));
        JobRunr.configure()
                .withJobFilter(logAllStateChangesFilter)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .useCarbonAwareScheduling("DE")
                .initialize();
    }
}
