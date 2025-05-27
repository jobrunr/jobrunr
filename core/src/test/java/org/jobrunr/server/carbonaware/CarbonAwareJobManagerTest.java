package org.jobrunr.server.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.mockito.internal.util.reflection.Whitebox;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
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
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.ITALY_2025_05_20_PT15M;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.UNKNOWN_AREA;
import static org.mockito.InstantMocker.mockTime;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class CarbonAwareJobManagerTest {

    @RegisterExtension
    static CarbonAwareApiWireMockExtension carbonAwareApiMock = new CarbonAwareApiWireMockExtension();

    @Test
    void testGetTimezoneReturnsSystemZoneIdByDefault() {
        assertThat(getCarbonAwareJobManager("UNKNOWN").getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsNextRefreshTimeWhenForecastIsNotAvailable() {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");

        assertThat(carbonAwareJobManager.getAvailableForecastEndTime()).isCloseTo(now(), within(1, SECONDS));
    }

    @Test
    void testGetTimeZoneReturnsCarbonIntensityForecastTimezoneWhenAvailable() {
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("DE");

        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsForecastEndPeriodWhenItsLaterThanNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("BE");

            assertThat(carbonAwareJobManager.getAvailableForecastEndTime()).isEqualTo("2024-07-11T22:00:00Z");
        }
    }

    @Test
    void testGetAvailableForecastEndTimeReturnsNextRefreshTimeWhenItsLaterThanForecastEndPeriod() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 12)))) {
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("BE");

            assertThat(carbonAwareJobManager.getAvailableForecastEndTime()).isEqualTo(now());
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryTakesIntoAccountNextForecastAvailableAtWhenSettingNextRefreshTime() {
        try (MockedStatic<Instant> ignored = mockTime(startOfDay(LocalDate.of(2024, 7, 11)))) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_11_FULL_2024_07_12);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("BE");
            CarbonIntensityApiClient carbonIntensityApiClient = getCarbonIntensityApiClient(carbonAwareJobManager);

            // THEN
            assertThat(carbonAwareJobManager.getNextRefreshTime()).isEqualTo(now());

            // WHEN
            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            // THEN
            verify(carbonIntensityApiClient).fetchCarbonIntensityForecast();
            assertThat(carbonAwareJobManager.getNextRefreshTime()).isCloseTo("2024-07-12T16:30:00.873318Z", within(randomRefreshTime(carbonAwareJobManager)));
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessaryDoesRunOnInitialCallThenWaitsUntilNextRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(17);
        try (var ignored = mockTime(dateTime)) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");
            CarbonIntensityApiClient carbonIntensityApiClient = getCarbonIntensityApiClient(carbonAwareJobManager);

            // THEN
            assertThat(carbonAwareJobManager.getNextRefreshTime()).isEqualTo(now());

            // WHEN
            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            // THEN
            verify(carbonIntensityApiClient, times(1)).fetchCarbonIntensityForecast();
            assertThat(carbonAwareJobManager.getNextRefreshTime()).isCloseTo(dateTime.withHour(19).toInstant(), within(randomRefreshTime(carbonAwareJobManager)));

            // WHEN
            clearInvocations(carbonAwareJobManager, carbonIntensityApiClient);
            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            // THEN
            verify(carbonIntensityApiClient, never()).fetchCarbonIntensityForecast();
        }
    }

    @Test
    void testUpdateCarbonIntensityForecastIfNecessarySetsNextRefreshTimeTheNextDayIfRunningAfterPlannedDailyRefreshTime() {
        ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(HOURS).withHour(20);
        try (var ignored = mockTime(dateTime)) {
            // GIVEN
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("DE", UNKNOWN_AREA);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager("DE");
            CarbonIntensityApiClient carbonIntensityApiClient = getCarbonIntensityApiClient(carbonAwareJobManager);

            // THEN
            assertThat(carbonAwareJobManager.getNextRefreshTime()).isEqualTo(now());

            // WHEN
            carbonAwareJobManager.updateCarbonIntensityForecastIfNecessary();

            verify(carbonIntensityApiClient, times(1)).fetchCarbonIntensityForecast();

            assertThat(carbonAwareJobManager.getNextRefreshTime()).isCloseTo(dateTime.plusDays(1).withHour(19).toInstant(), within(randomRefreshTime(carbonAwareJobManager)));
        }
    }

    @Test
    void testMoveToNextStateAlsoWorksForPT15MIntensityIntervals() {
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("IT", ITALY_2025_05_20_PT15M);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("IT");
        try (MockedStatic<Instant> ignored = mockTime(Instant.parse("2025-05-20T13:00:00.000Z"))) {
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().minus(1, HOURS), now().plus(3, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job);

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt(Instant.parse("2025-05-20T12:00:00.000Z"), "At the best moment to minimize carbon impact.");
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
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_PARTIAL_2024_07_12);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("BE");
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
        carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("BE");
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
            carbonAwareApiMock.mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
            CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManagerWithForecast("BE");
            Job job = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.before(now().plus(4, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job);

            assertThat(job)
                    .hasStates(AWAITING, SCHEDULED)
                    .isScheduledAt("2024-07-11T01:00:00Z");
        }
    }

    private CarbonAwareJobManager getCarbonAwareJobManagerWithForecast(String areaCode) {
        CarbonAwareJobManager carbonAwareJobManager = getCarbonAwareJobManager(areaCode);
        carbonAwareJobManager.updateCarbonIntensityForecast();
        return carbonAwareJobManager;
    }

    private CarbonAwareJobManager getCarbonAwareJobManager(String areaCode) {
        CarbonAwareConfigurationReader carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareApiMock.getCarbonAwareConfigurationForAreaCode(areaCode));
        CarbonIntensityApiClient carbonIntensityApiClient = spy(new CarbonIntensityApiClient(carbonAwareConfiguration, new JacksonJsonMapper()));
        return spy(new CarbonAwareJobManager(carbonAwareConfiguration, carbonIntensityApiClient));
    }

    private CarbonIntensityApiClient getCarbonIntensityApiClient(CarbonAwareJobManager carbonAwareJobManager) {
        return Whitebox.getInternalState(carbonAwareJobManager, "carbonIntensityApiClient");
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }

    private Duration randomRefreshTime(CarbonAwareJobManager carbonAwareJobManager) {
        return Whitebox.getInternalState(carbonAwareJobManager, "randomRefreshTimeOffset");
    }
}
