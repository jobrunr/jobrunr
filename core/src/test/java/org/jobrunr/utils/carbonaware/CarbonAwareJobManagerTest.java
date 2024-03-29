package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.*;
import org.mockito.DatetimeMocker;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.utils.carbonaware.CarbonAwarePeriod.*;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {

    StorageProvider storageProvider;
    @BeforeEach
    void setUpStorageProvider() {
        storageProvider = new InMemoryStorageProvider();
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineNow_shouldThrowException() {
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        assertThatThrownBy(() -> BackgroundJob.scheduleCarbonAware(before(now()),
                () -> System.out.println("Hello from CarbonAware job!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'to' must be at least 3 hours in the future to use Carbon Aware Scheduling");
    }


    @Test
    public void testScheduleCarbonAwareJob_withDeadlineIn2Hours_shouldThrowException() {
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        assertThatThrownBy(() -> BackgroundJob.scheduleCarbonAware(before(now().plus(2, ChronoUnit.HOURS)),
                () -> System.out.println("Hello from CarbonAware job!")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'to' must be at least 3 hours in the future to use Carbon Aware Scheduling");
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_andTimeBefore14_shouldWait() {
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2026-10-10T08:00:00Z")) {
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(now().plus(1, ChronoUnit.DAYS)),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadlineTomorrow_andOutdatedData_andTimeAfter14_shouldScheduleNow() {
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_2024_03_14);
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
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T11:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(after(LocalDate.of(2024,3, 14)),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_and12HoursData_shouldScheduleAtIdealMoment() {
        mockApiResponseAndInitializeJobRunr(1000, "BE", CarbonApiMockResponses.BELGIUM_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-14T08:00:00Z"), "Europe/Brussels")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(Instant.parse("2024-03-15T23:00:00Z")),
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
        mockApiResponseAndInitializeJobRunr(1000, "BE", CarbonApiMockResponses.BELGIUM_2024_03_14);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-03-14T08:00:00Z");
            MockedStatic<ZonedDateTime> b = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-03-14T08:00:00Z"), "Europe/Brussels")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(Instant.parse("2024-03-16T23:00:00Z")),
                    () -> System.out.println("Hello from CarbonAware job!"));
            await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, SCHEDULED);
        }
    }


    @Test
    public void testScheduleCarbonAwareJob_withDeadlineIn7Days_andNoData_shouldWait() {
        mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_NO_DATA);
        try(MockedStatic<Instant> a = InstantMocker.mockTime("2024-01-01T08:00:00Z")){
            JobId jobId = BackgroundJob.scheduleCarbonAware(between(Instant.parse("2024-01-01T07:00:00Z"), Instant.parse("2024-01-08T15:00:00Z")),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadlineIn7Days_andNoData_shouldWait"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsAfter14_shouldScheduleNow() {
        try (MockedStatic<ZonedDateTime> a = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-01-01T15:00:00Z"), "Europe/Brussels");
             MockedStatic<Instant> b = InstantMocker.mockTime("2024-01-01T15:00:00Z")) {
            mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_NO_DATA);
            JobId jobId = BackgroundJob.scheduleCarbonAware(before(Instant.parse("2024-01-02T19:00:00Z")),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsAfter14_shouldScheduleNow"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING, ENQUEUED, PROCESSING, SUCCEEDED);
        }
    }

    @Test
    public void testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsBefore14_shouldWait() {
        try (MockedStatic<ZonedDateTime> a = DatetimeMocker.mockZonedDateTime(ZonedDateTime.parse("2024-01-01T08:00:00Z"), "Europe/Brussels");
             MockedStatic<Instant> b = InstantMocker.mockTime("2024-01-01T08:00:00Z")) {
            mockApiResponseAndInitializeJobRunr(200, "DE", CarbonApiMockResponses.GERMANY_NO_DATA);
            JobId jobId = BackgroundJob.scheduleCarbonAware(after(LocalDate.now().plusDays(1)),
                    () -> System.out.println("Hello from CarbonAware job: testScheduleCarbonAwareJob_withDeadline1Day_andNoData_andTimeIsBefore14_shouldWait"));
            await().atMost(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == AWAITING);
            assertThat(storageProvider.getJobById(jobId)).hasStates(AWAITING);
        }
    }


    private void mockApiResponseAndInitializeJobRunr(int pollIntervalMs, String area, String mockResponse) {
        storageProvider = new InMemoryStorageProvider();
        mockResponseWhenRequestingArea(area, mockResponse);
        initializeJobRunr(pollIntervalMs, area, storageProvider);
    }
}
