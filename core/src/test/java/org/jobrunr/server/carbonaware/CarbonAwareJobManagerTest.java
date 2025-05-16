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
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {
    @Test
    void testGetTimezoneReturnsSystemZoneIdByDefault() {
        assertThat(getCarbonAwareJobManager("UNKNOWN").getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void testGetTimeZoneReturnsCarbonIntensityForecastTimezoneWhenAvailable() {
        mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        carbonAwareJobManager.updateCarbonIntensityForecast();

        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsNextRefreshTimeWhenForecastIsNotAvailable() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        assertThat(carbonAwareJobManager.getAvailableForecastEndTime())
                .isCloseTo(Instant.now(), within(1, SECONDS));
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsForecastEndPeriodWhenItsLaterThanNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getAvailableForecastEndTime())
                    .isEqualTo("2024-07-11T22:00:00Z");
        }
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsNextRefreshTimeWhenItsLaterThanForecastEndPeriod() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 12)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            carbonAwareJobManager.updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getAvailableForecastEndTime())
                    .isEqualTo(Instant.now());
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryTakesIntoAccountNextForecastAvailableAtWhenSettingNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12);
            CarbonAwareJobManager carbonAwareJobManager = spy(getCarbonAwareJobManager("BE"));

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isEqualTo(Instant.now());

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager).updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo("2024-07-12T16:30:00.873318Z", within(30, MINUTES));
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryDoesRunOnInitialCallThenWaitsUntilNextRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(17);
        try (var ignored1 = mockZonedDateTime(dateTime, ZoneId.systemDefault()); var ignored2 = mockTime(dateTime.toInstant())) {
            mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = spy(getCarbonAwareJobManager("DE"));

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isEqualTo(Instant.now());

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
                    .isEqualTo(Instant.now());

            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonAwareJobManager).updateCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime())
                    .isCloseTo(dateTime.plusDays(1).withHour(19).toInstant(), within(30, MINUTES));
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
    void testMoveToNextStateSchedulesCarbonAwaitingJobsImmediatelyThatHaveDeadlineInThePast() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_12);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
        carbonAwareJobManager.updateCarbonIntensityForecast();
        LocalDate localDate = LocalDate.of(2024, 7, 11);
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate))) {
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().minus(8, HOURS), now().minus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job);

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now(), "Passed its deadline, scheduling now.");
        }
    }

    @Test
    void testMoveToNextStateSchedulesCarbonAwaitingJobsImmediatelyIfMarginIsSmallerThanMinimumScheduleMargin() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
        carbonAwareJobManager.updateCarbonIntensityForecast();
        LocalDate localDate = LocalDate.of(2024, 7, 11);
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(localDate))) {
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plusSeconds(300))).build();
            carbonAwareJobManager.moveToNextState(job);

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(now(), "Not enough margin (PT5M) to be scheduled carbon aware.");
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
                    .isScheduledAt("2024-07-11T01:00:00Z");
        }
    }

    private CarbonAwareJobManager getCarbonAwareJobManager(String areaCode) {
        return new CarbonAwareJobManager(getCarbonAwareConfigurationForAreaCode(areaCode), getJsonMapper());
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }
}
