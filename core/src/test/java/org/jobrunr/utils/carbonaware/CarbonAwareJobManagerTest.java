package org.jobrunr.utils.carbonaware;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.*;
import org.mockito.DatetimeMocker;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static java.time.Instant.now;
import static java.time.LocalDate.of;
import static org.assertj.core.api.BDDAssertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.utils.carbonaware.CarbonAwarePeriod.before;
import static org.jobrunr.utils.carbonaware.CarbonAwarePeriod.beforeStartOf;
import static org.jobrunr.utils.carbonaware.CarbonAwareSchedulingTestUtils.mockCarbonAwareConf;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {

    StorageProvider storageProvider;

    @BeforeEach
    void setUpStorageProvider() {
        storageProvider = new InMemoryStorageProvider();
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineNow_shouldScheduleNow() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        JobId jobId = BackgroundJob.scheduleCarbonAware(before(now().plusSeconds(1)),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }


    @Test
    public void testScheduleCarbonAwareJob_withDeadlineIn10Minutes_shouldScheduleNow() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        JobId jobId = BackgroundJob.scheduleCarbonAware(before(now().plus(10, ChronoUnit.MINUTES)),
                () -> System.out.println("Hello from CarbonAware job!"));
        await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_andTimeBefore14_shouldWait() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2026-10-10T08:00:00Z")) {
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(now().plus(1, ChronoUnit.DAYS)),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_andTimeAfter14_shouldScheduleNow() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2026-10-10T15:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2026-10-10T15:00:00Z"), "Europe/Brussels")) {
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(now().plus(1, ChronoUnit.DAYS)),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withExpiredDeadline_shouldScheduleImmediately() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T11:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(beforeStartOf(LocalDate.of(2024,3, 14)),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment() {
        mockApiResponseAndInitializeJobRunr("BE", CarbonApiMockResponses.BELGIUM_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-14T08:00:00Z"), "Europe/Brussels")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-15T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment"));
            await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            Job job = storageProvider.getJobById(jobId);
            assertThat(job).hasStates(AWAITING, SCHEDULED);
            assertThat(job).hasUpdatedAtCloseTo(Instant.parse("2024-03-14T08:00:00Z"), within(1, ChronoUnit.SECONDS));
            ScheduledState scheduledState = job.getJobState();
            assertThat(scheduledState.getScheduledAt()).isEqualTo(Instant.parse("2024-03-14T12:00:00Z"));
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline2Days_and12HoursData_shouldScheduleAtIdealMoment() {
        mockApiResponseAndInitializeJobRunr("BE", CarbonApiMockResponses.BELGIUM_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-14T08:00:00Z"), "Europe/Brussels")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-03-16T23:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }


    @Test
    public void testScheduleCarbonAwareJob_withDeadlineIn7Days_andNoData_shouldWait() {
        mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-01-01T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-01-01T07:00:00Z"), Instant.parse("2024-01-08T15:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadlineIn7Days_andNoData_shouldWait"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsAfter14_shouldScheduleNow() {
        try (MockedStatic<ZonedDateTime> a = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-01-01T15:00:00Z"), "Europe/Brussels");
             MockedStatic<Instant> b = InstantMocker.mockTime("2024-01-01T15:00:00Z")) {
            mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
            JobId jobId = BackgroundJob.scheduleCarbonAware(Instant.parse("2024-01-02T19:00:00Z"),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsAfter14_shouldScheduleNow"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsBefore14_shouldWait() {
        try (MockedStatic<ZonedDateTime> a = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-01-01T08:00:00Z"), "Europe/Brussels");
             MockedStatic<Instant> b = InstantMocker.mockTime("2024-01-01T08:00:00Z")) {
            mockApiResponseAndInitializeJobRunr("DE", CarbonApiMockResponses.GERMANY_NO_DATA);
            JobId jobId = BackgroundJob.scheduleCarbonAware(now().plus(1, ChronoUnit.DAYS),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsBefore14_shouldWait"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    public void testUpdateAwaitingJobs() {
        //ARRANGE
        String area = "DE";
        JobScheduler jobScheduler = initializeJobRunr(200, area);
        JobId jobId1 = BackgroundJob.scheduleCarbonAware(Instant.parse("2500-01-01T00:00:00Z"), Instant.parse("2500-01-01T23:00:00Z"),
                () -> System.out.println("1. This job should be scheduled at 12:00"));
        JobId jobId2 = BackgroundJob.scheduleCarbonAware(Instant.parse("2600-01-01T00:00:00Z"), Instant.parse("2600-01-01T23:00:00Z"),
                () -> System.out.println("2. This job should wait"));
        JobId jobId3 = BackgroundJob.scheduleCarbonAware(Instant.parse("2400-01-01T00:00:00Z"), Instant.parse("2400-01-01T23:00:00Z"),
                () -> System.out.println("3. This job should be run immediately"));
        JobId jobId4 = BackgroundJob.scheduleCarbonAware(Instant.parse("2500-01-01T15:00:00Z"), Instant.parse("2500-01-01T23:00:00Z"),
                () -> System.out.println("4. This job should be scheduled at 22:00"));

        assertThat(storageProvider.getJobById(jobId1)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId2)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId3)).hasStates(AWAITING);
        assertThat(storageProvider.getJobById(jobId4)).hasStates(AWAITING);

        CarbonAwareJobManager carbonAwareScheduler = jobScheduler.getCarbonAwareScheduler();
        mockResponseWhenRequestingArea(area, CarbonApiMockResponses.GERMANY_2500_01_01, wireMockServer);
        try(MockedStatic<CarbonAwareConfiguration> conf = mockCarbonAwareConf(area);
            MockedStatic<Instant> a = InstantMocker.mockTime("2500-01-01T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2500-01-01T08:00:00Z"), "Europe/Brussels")) {
            carbonAwareScheduler.updateDayAheadEnergyPrices(Optional.of(area));
            //ACT
            carbonAwareScheduler.updateAwaitingJobs();

            //ASSERT
            Job job1 = storageProvider.getJobById(jobId1);
            assertThat(job1).hasStates(AWAITING, SCHEDULED);
            assertThat(((ScheduledState) job1.getJobState()).getScheduledAt()).isEqualTo(Instant.parse("2500-01-01T12:00:00Z"));

            assertThat(storageProvider.getJobById(jobId2)).hasStates(AWAITING);

            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId3).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId3)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);

            Job job4 = storageProvider.getJobById(jobId4);
            assertThat(job4).hasStates(AWAITING, SCHEDULED);
            assertThat(((ScheduledState) job4.getJobState()).getScheduledAt()).isEqualTo(Instant.parse("2500-01-01T22:00:00Z"));

        }
    }


    private void mockApiResponseAndInitializeJobRunr(String area, String mockResponse) {
        mockResponseWhenRequestingArea(area, mockResponse);
        initializeJobRunr(area);
    }

    private JobScheduler initializeJobRunr(String area) {
        return JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useCarbonAwareScheduling(usingStandardCarbonAwareConfiguration().andArea(area))
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(Duration.ofMillis(200)))
                .initialize()
                .getJobScheduler();
    }
}
