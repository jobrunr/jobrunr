package org.jobrunr.server.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.GERMANY_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.UNKNOWN_AREA;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {
    @Test
    void testGetTimezoneReturnsSystemZoneIdByDefault() {
        assertThat(getCarbonAwareJobManager("DE").getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void testGetTimeZoneReturnsCarbonIntensityForecastTimezoneWhenAvailable() {
        mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        carbonAwareJobManager.updateCarbonIntensityForecast();

        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void testGetDefaultDailyRefreshTimeShouldGiveResultBetweenGivenRefreshTimeAndAnHourLater() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        Instant result = carbonAwareJobManager.getDefaultDailyRefreshTime();
        ZonedDateTime expectedTime = ZonedDateTime.now(carbonAwareJobManager.getTimeZone())
                .truncatedTo(HOURS)
                .withHour(19);

        assertThat(result).isAfterOrEqualTo(expectedTime.toInstant());
        assertThat(result).isBefore(expectedTime.plusHours(1).toInstant());
    }

    @Test
    void testGetDefaultDailyRefreshTimeShouldGiveTheSameResultOnConsecutiveCalls() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        Instant firstCall = carbonAwareJobManager.getDefaultDailyRefreshTime();
        Instant secondCall = carbonAwareJobManager.getDefaultDailyRefreshTime();

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    void testGetTheLaterBetweenForecastEndPeriodAndNextRefreshTimeReturnsNextRefreshTimeWhenForecastIsNotAvailable() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime())
                .isCloseTo(Instant.now(), within(1, SECONDS));
    }

    @Test
    void testGetTheLaterBetweenForecastEndPeriodAndNextRefreshTimeReturnsForecastEndPeriodWhenItsLaterThanNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime())
                    .isEqualTo("2024-07-11T22:00:00Z");
        }
    }

    @Test
    void testGetTheLaterBetweenForecastEndPeriodAndNextRefreshTimeReturnsNextRefreshTimeWhenItsLaterThanForecastEndPeriod() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 12)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime())
                    .isCloseTo(Instant.now(), within(1, SECONDS));
        }
    }

    @Test
    void testConstructingACarbonAwareJobManagerLoadForecastAndScheduleForecastUpdateAndSetTheNextRefreshTime() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStatic<ZonedDateTime> ignored1 = mockZonedDateTime(currentTime, ZoneId.systemDefault());
             MockedStatic<Instant> ignored2 = mockTime(currentTime.toInstant())
        ) {
            mockResponseWhenRequestingAreaCode("BE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);

            assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime()).isEqualTo(now().plusSeconds(1));

            await().atMost(Duration.ofMillis(1500)).untilAsserted(() -> verifyApiCalls("BE", 1));
            await().atMost(Duration.ofMillis(500)).untilAsserted(() -> {
                assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime()).isCloseTo(carbonAwareJobManager.getDefaultDailyRefreshTime(), within(30, MINUTES));
                assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime().getEpochSecond() % 5).isZero();
            });
        }
    }

    @Test
    void testForecastUpdatesAreAutomaticallyScheduledAfterInitialLoad() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        try (MockedStatic<ZonedDateTime> ignored1 = mockZonedDateTime(currentTime, ZoneId.systemDefault());
             MockedStatic<Instant> ignored2 = mockTime(currentTime.toInstant())
        ) {
            mockResponseWhenRequestingAreaCodeWithScenarios("BE", BELGIUM_2024_07_11, UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);

            await().atMost(Duration.ofMillis(5000)).untilAsserted(() -> verifyApiCalls("BE", 2));
            await().atMost(Duration.ofMillis(500)).untilAsserted(() -> {
                assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime()).isCloseTo(carbonAwareJobManager.getDefaultDailyRefreshTime().plus(1, DAYS), within(30, MINUTES));
                assertThat(carbonAwareJobManager.getTheLaterBetweenForecastEndPeriodAndNextRefreshTime().getEpochSecond() % 5).isZero();
            });
        }
    }

    @Test
    void testMoveToNextStateThrowsAnExceptionIfGivenJobsThatAreNotCarbonAwaiting() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        Job job = anEnqueuedJob().build();

        assertThatCode(() -> carbonAwareJobManager.moveToNextState(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only jobs in CarbonAwaitingState can move to a next state");
    }

    @Test
    void testMoveToNextStateEnqueuesCarbonAwaitingJobsThatHaveDeadlineInThePast() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
        carbonAwareJobManager.updateCarbonIntensityForecast();
        LocalDate localDate = LocalDate.of(2024, 7, 11);
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
    void testMoveToNextStateSchedulesCarbonAwaitingJobsAtPreferredInstantWhenCarbonIntensityForecastAreNotAvailable() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.now()))) {
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");
            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING, ENQUEUED);

            Job job2 = aJob().withState(new CarbonAwareAwaitingState(now().plus(2, HOURS), now(), now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job2);

            assertThat(job2).hasStates(AWAITING, SCHEDULED).isScheduledAt(now().plus(2, HOURS));
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsWhenCarbonIntensityForecastAreAvailable() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job);

            assertThat(job).hasStates(AWAITING, SCHEDULED);
        }
    }

    @Test
    void testMoveToNextStateDoesNotModifyJobsThatAreCarbonAwaitingButDeadlineIsAfterTomorrowRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(2, DAYS), now().plus(4, DAYS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING);
        }
    }

    @Test
    void testMoveToNextStateDoesNotModifyJobsThatAreCarbonAwaitingButDeadlineIsNextDayAndCurrentTimeIsBeforeRefreshTime() {
        Instant startOfDay = startOfDay(LocalDate.of(2024, 7, 11));
        try (MockedStatic<Instant> ignored1 = mockTime(startOfDay);
             MockedStatic<ZonedDateTime> ignored2 = mockZonedDateTime(startOfDay.atZone(ZoneId.of("Europe/Brussels")), "Europe/Brussels")) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(1, DAYS), now().plus(1, DAYS).plus(2, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING);
        }
    }

    private CarbonAwareJobManager getCarbonAwareJobManager(String areaCode) {
        return new CarbonAwareJobManager(setupCarbonAwareConfiguration(areaCode), jsonMapper);
    }

    private CarbonAwareConfiguration setupCarbonAwareConfiguration(String areaCode) {
        return usingStandardCarbonAwareConfiguration().andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl).andAreaCode(areaCode);
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }
}
