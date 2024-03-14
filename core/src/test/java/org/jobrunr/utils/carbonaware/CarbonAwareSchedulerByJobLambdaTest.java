package org.jobrunr.utils.carbonaware;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobId;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.LogAllStateChangesFilter;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.mapper.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.time.Duration.ofMillis;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_SECOND;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public class CarbonAwareSchedulerByJobLambdaTest {
    private TestService testService;
    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private static final String everySecond = "*/1 * * * * *";
    private LogAllStateChangesFilter logAllStateChangesFilter;

    private static WireMockServer wireMockServer;
    private static JsonMapper jsonMapper;
//    @BeforeAll
//    static void beforeAll() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModule(new JavaTimeModule());
//        jsonMapper = new JacksonJsonMapper(mapper);
//        wireMockServer = new WireMockServer(10000);
//        wireMockServer.start();
//    }

    @BeforeEach
    void setUpTests() {
        testService = new TestService();
        testService.reset();
        storageProvider = new InMemoryStorageProvider();
        logAllStateChangesFilter = new LogAllStateChangesFilter();
        JobRunr.configure()
                .withJobFilter(logAllStateChangesFilter)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .useCarbonAwareScheduling("DE")
                .initialize();
//        wireMockServer.resetAll();
    }
//    @AfterAll
//    static void tearDown() {
//        wireMockServer.stop();
//    }

    @Test
    public void testScheduleCarbonAwareJobNow() {
//        wireMockServer.stubFor(WireMock.get(urlEqualTo("/carbon-intensity/v1/day-ahead-energy-prices?area=BE"))
//                .willReturn(aResponse()
//                        .withHeader("Content-Type", "application/json")
//                        .withBody(CarbonApiMockResponses.BELGIUM_2024_03_12)));

        JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.now(),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }
}
