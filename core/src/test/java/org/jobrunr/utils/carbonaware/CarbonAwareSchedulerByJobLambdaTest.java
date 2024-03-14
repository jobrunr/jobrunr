package org.jobrunr.utils.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InstantMocker;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.time.Duration.ofMillis;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public class CarbonAwareSchedulerByJobLambdaTest {
    private TestService testService;
    private static StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private static final String everySecond = "*/1 * * * * *";
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
    public void testScheduleCarbonAwareJobNow() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now(),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    public void testScheduleCarbonAwareJobInOneMinute() {
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plusSeconds(60),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andBestHourIn4Hours() {
        InstantMocker.mockTime("2024-03-14T08:00:00Z");

        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plus(1, ChronoUnit.DAYS),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_shouldWaitUntilDeadline() {
        // in this case we wait until the deadline, in case data becomes available in the meantime
        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now().plus(1, ChronoUnit.DAYS),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
    }

    @Test
    public void testScheduleCarbonAwareJob_withExpiredDeadline_shouldScheduleImmediately() {
        InstantMocker.mockTime("2024-03-14T08:00:00Z");

        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-14T08:00:00Z"),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }
}
