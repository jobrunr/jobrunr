package org.jobrunr.scheduling.carbonaware;

import org.jobrunr.jobs.carbonaware.AbstractCarbonAwareWiremockTest;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestJobRequestWithoutJobAnnotation;
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
import java.util.UUID;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.scheduling.JobBuilder.aJob;
import static org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod.before;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

// TODO add these to BackgroundJobRequest tests?
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
             MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-12T11:00:00Z"), "Europe/Brussels")) {
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
        try (MockedStatic<Instant> a = InstantMocker.mockTime(ZonedDateTime.parse("2024-03-12T11:00:00Z"), "Europe/Brussels");
             MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-12T11:00:00Z"), "Europe/Brussels")) {
            BackgroundJobRequest.create(aJob()
                    .withId(jobId)
                    .withName("My Job Name")
                    .withAmountOfRetries(3)
                    .withJobRequest(new TestJobRequestWithoutJobAnnotation("carbonAware job from createViaBuilder"))
                    .scheduleCarbonAware(before(now().plus(10, HOURS)))

            );

            await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);

            assertThat(storageProvider.getJobById(jobId))
                    .hasJobName("My Job Name")
                    .hasAmountOfRetries(3)
                    .hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }


    @Test
    void testScheduleCarbonAware_withFromAt03_andNoData_andTimeAfter18_shouldScheduleAtStart() {
        UUID uuid = UUID.randomUUID();
        Instant from = Instant.parse("2024-03-13T03:00:00Z");
        try (MockedStatic<Instant> a = InstantMocker.mockTime(ZonedDateTime.parse("2024-03-12T21:10:00Z"), "Europe/Brussels");
             MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-12T21:10:00Z"), "Europe/Brussels")) {

            JobId jobId = BackgroundJobRequest.scheduleCarbonAware(uuid, CarbonAwarePeriod.between(from, from.plus(10, HOURS)),
                    new TestJobRequest("from scheduleCarbonAware"));

            await().atMost(FIVE_SECONDS).until(() ->
                    storageProvider.getJobById(jobId).getState() == SCHEDULED);

            assertThat(storageProvider.getJobById(uuid))
                    .isScheduledAt(from);
        }
    }
}
