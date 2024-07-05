package org.jobrunr.jobs.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.carbonaware.CarbonApiMockResponses.BELGIUM_2024_03_12;
import static org.jobrunr.jobs.carbonaware.CarbonApiMockResponses.GERMANY_2024_03_14;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {
    private CarbonAwareJobManager carbonAwareJobManager;

    @BeforeEach
    void setUp() {
        carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("DE"), jsonMapper);
    }

    @Test
    void testGetDailyRefreshTimeShouldGiveResultBetweenGivenRefreshTimeAndAnHourLater() {
        ZonedDateTime result = carbonAwareJobManager.getDailyRefreshTime();
        ZonedDateTime expectedTime = ZonedDateTime.now(carbonAwareJobManager.getTimeZone())
                .truncatedTo(HOURS)
                .withHour(18);

        assertThat(result).isAfterOrEqualTo(expectedTime);
        assertThat(result).isBefore(expectedTime.plusHours(1));
    }

    @Test
    void testGetDailyRefreshTimeShouldGiveTheSameResultOnConsecutiveCalls() {
        ZonedDateTime firstCall = carbonAwareJobManager.getDailyRefreshTime();
        ZonedDateTime secondCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    void testDailyRefreshTimeShouldGiveResultsIn5SecondsBuckets() {
        ZonedDateTime firstCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall.toInstant().getEpochSecond() % 5).isZero();
    }

    @Test
    void testGetTimezoneReturnsSystemZoneIdByDefault() {
        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void testGetTimeZoneReturnsDayAheadPricesTimezoneWhenAvailable() {
        mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_03_14);

        carbonAwareJobManager.updateDayAheadEnergyPrices();

        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }


    @Test
    void testMoveToNextStateDoesNotModifyJobsThatAreNotCarbonAwaiting() {
        Job job = anEnqueuedJob().build();

        carbonAwareJobManager.moveToNextState(job);

        assertThat(job).hasState(ENQUEUED);
    }

    @Test
    void testMoveToNextStateEnqueuesCarbonAwaitingJobsThatHaveDeadlineInThePast() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_03_12);
        carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
        carbonAwareJobManager.updateDayAheadEnergyPrices();
        LocalDate localDate = LocalDate.of(2024, 3, 12);
        Job job;
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate))) {
            job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();
        }

        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate).plus(4, HOURS))) {
            carbonAwareJobManager.moveToNextState(job);

            assertThat(job).hasStates(AWAITING, ENQUEUED);
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsAtPreferredInstantWhenDayAheadPricesAreNotAvailable() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.now()))) {
            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING, ENQUEUED);

            Job job2 = aJob().withState(new CarbonAwareAwaitingState(now().plus(2, HOURS), now(), now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job2);

            assertThat(job2).hasStates(AWAITING, SCHEDULED).isScheduledAt(now().plus(2, HOURS));
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsWhenDayAheadPricesAreAvailable() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 3, 12)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_03_12);
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
            carbonAwareJobManager.updateDayAheadEnergyPrices();
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job);

            assertThat(job).hasStates(AWAITING, SCHEDULED);
        }
    }

    @Test
    void testMoveToNextStateDoesNotModifyJobsThatAreCarbonAwaitingButDeadlineIsAfterTomorrowRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 3, 12)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_03_12);
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
            carbonAwareJobManager.updateDayAheadEnergyPrices();

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(2, DAYS), now().plus(4, DAYS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING);
        }
    }

    @Test
    void testMoveToNextStateDoesNotModifyJobsThatAreCarbonAwaitingButDeadlineIsNextDayAndCurrentTimeIsBeforeRefreshTime() {
        Instant startOfDay = startOfDay(LocalDate.of(2024, 3, 12));
        try (MockedStatic<Instant> ignored1 = mockTime(startOfDay);
             MockedStatic<ZonedDateTime> ignored2 = mockZonedDateTime(startOfDay.atZone(ZoneId.of("Europe/Brussels")), "Europe/Brussels")) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_03_12);
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
            carbonAwareJobManager.updateDayAheadEnergyPrices();

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(1, DAYS), now().plus(1, DAYS).plus(2, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING);
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsWhoseDeadlineIsNextDayWhenThereIsNoDataAndCurrentTimeIsAfterRefreshTime() {
        try (MockedStatic<Instant> ignored1 = mockTime(carbonAwareJobManager.getDailyRefreshTime().toInstant());
             MockedStatic<ZonedDateTime> ignored2 = mockZonedDateTime(carbonAwareJobManager.getDailyRefreshTime(), ZoneId.systemDefault())
        ) {
            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(1, DAYS), now().plus(1, DAYS).plus(2, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING, SCHEDULED);
        }
    }

    private CarbonAwareConfiguration setupCarbonAwareConfiguration(String areaCode) {
        return usingStandardCarbonAwareConfiguration().andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl).andAreaCode(areaCode);
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }
}
