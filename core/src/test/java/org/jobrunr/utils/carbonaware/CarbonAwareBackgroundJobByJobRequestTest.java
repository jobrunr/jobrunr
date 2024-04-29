package org.jobrunr.utils.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForIoC;
import org.jobrunr.stubs.TestServiceInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.DatetimeMocker;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;
import org.slf4j.MDC;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.scheduling.JobBuilder.aJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.utils.carbonaware.CarbonAwarePeriod.before;


public class CarbonAwareBackgroundJobByJobRequestTest extends AbstractCarbonAwareWiremockTest {

    StorageProvider storageProvider;
    BackgroundJobServer backgroundJobServer;
    TestServiceForIoC testServiceForIoC;
    TestServiceInterface testServiceInterface;


    @BeforeEach
    void setUpStorageProvider() {

        storageProvider = new InMemoryStorageProvider();
        testServiceForIoC = new TestServiceForIoC("a constructor arg");
        testServiceInterface = testServiceForIoC;
        SimpleJobActivator jobActivator = new SimpleJobActivator(testServiceForIoC, new TestService());
        try (MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-12T11:00:00Z");
             MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-12T11:00:00Z", DateTimeFormatter.ISO_DATE_TIME), "Europe/Brussels")) {
            JobRunr.configure()
                    .useJobActivator(jobActivator)
                    .useStorageProvider(storageProvider)
                    .useCarbonAwareScheduling(usingStandardCarbonAwareConfiguration().andAreaCode("BE"))
                    .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                    .initialize();
        }
        backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    @AfterEach
    public void cleanUp() {
        MDC.clear();
        backgroundJobServer.stop();
        storageProvider.close();
    }


    @Test
    void testCreateViaBuilder_withNoData_andTodayIsDeadline_shouldEnqueue() {
        UUID jobId = UUID.randomUUID();
        try (MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-12T11:00:00Z");
             MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-12T11:00:00Z", DateTimeFormatter.ISO_DATE_TIME), "Europe/Brussels")) {
            BackgroundJob.create(aJob()
                    .withId(jobId)
                    .withName("My Job Name")
                    .withAmountOfRetries(3)
                    .<TestService>withDetails(x -> x.doWorkAndReturnResult("some string"))
                    .scheduleCarbonAware(before(now().plus(10, HOURS)))

            );

            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);

            assertThat(storageProvider.getJobById(jobId))
                    .hasJobName("My Job Name")
                    .hasAmountOfRetries(3)
                    .hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }


}
