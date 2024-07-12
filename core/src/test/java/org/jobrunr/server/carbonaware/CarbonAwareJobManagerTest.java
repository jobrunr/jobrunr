package org.jobrunr.server.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_PARTIAL_2024_07_12;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.GERMANY_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.UNKNOWN_AREA;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    void testGetTheLaterOfForecastEndAndNextRefreshTimeReturnsNextRefreshTimeWhenForecastIsNotAvailable() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        assertThat(carbonAwareJobManager.getTheLaterOfForecastEndAndNextRefreshTime())
                .isCloseTo(Instant.now(), within(1, SECONDS));
    }

    @Test
    void testGetTheLaterOfForecastEndAndNextRefreshTimeReturnsForecastEndPeriodWhenItsLaterThanNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getTheLaterOfForecastEndAndNextRefreshTime())
                    .isEqualTo("2024-07-11T22:00:00Z");
        }
    }

    @Test
    void testGetTheLaterOfForecastEndAndNextRefreshTimeReturnsNextRefreshTimeWhenItsLaterThanForecastEndPeriod() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 12)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getTheLaterOfForecastEndAndNextRefreshTime())
                    .isCloseTo(Instant.now(), within(1, SECONDS));
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryDoesRunOnInitialCallThenWaitsUntilNextRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(17);
        try (var ignored1 = mockZonedDateTime(dateTime, ZoneId.systemDefault()); var ignored2 = mockTime(dateTime.toInstant())) {
            mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = spy(getCarbonAwareJobManager("DE"));

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(Instant.now(), within(1, SECONDS));

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager).updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(dateTime.withHour(19).toInstant(), within(30, MINUTES));

            clearInvocations(carbonAwareJobManager);

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager, times(0)).updateCarbonIntensityForecast();
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessarySetsNextRefreshTimeTheNextDayIfRunningAfterPlannedDailyRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(20);
        try (var ignored1 = mockZonedDateTime(dateTime, ZoneId.systemDefault()); var ignored2 = mockTime(dateTime.toInstant())) {
            mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = spy(getCarbonAwareJobManager("DE"));

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(Instant.now(), within(1, SECONDS));

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager).updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(dateTime.plusDays(1).withHour(19).toInstant(), within(30, MINUTES));
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryTakesIntoAccountNextForecastAvailableAtWhenSettingNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12);
            CarbonAwareJobManager carbonAwareJobManager = spy(getCarbonAwareJobManager("BE"));

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(Instant.now(), within(1, SECONDS));

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager).updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo("2024-07-12T16:30:00.873318Z", within(30, MINUTES));
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
    void testMoveToNextStateSchedulesImmediatelyCarbonAwaitingJobsThatHaveDeadlineInThePast() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_12);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
        carbonAwareJobManager.updateCarbonIntensityForecast();
        LocalDate localDate = LocalDate.of(2024, 7, 11);
        Job job;
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate))) {
            job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();
        }

        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate).plus(4, HOURS))) {
            carbonAwareJobManager.moveToNextState(job);

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().minus(4, HOURS));
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsAtPreferredInstantWhenCarbonIntensityForecastAreNotAvailable() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.now()))) {
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");
            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now());

            Job job2 = aJob().withState(new CarbonAwareAwaitingState(now().plus(2, HOURS), now(), now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job2);

            assertThat(job2)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now().plus(2, HOURS));
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

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt("2024-07-11T02:00:00Z");
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
