package org.jobrunr.server.carbonaware;

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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.AWAITING;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.GERMANY_2024_07_11;
import static org.jobrunr.server.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;
import static org.mockito.DatetimeMocker.mockZonedDateTime;
import static org.mockito.InstantMocker.mockTime;

public class CarbonAwareJobManagerTest extends AbstractCarbonAwareWiremockTest {
    private CarbonAwareJobManager carbonAwareJobManager;

    @BeforeEach
    void setUp() {
        carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("DE"), jsonMapper);
    }

    @Test
    void testGetTimezoneReturnsSystemZoneIdByDefault() {
        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.systemDefault());
    }

    @Test
    void testGetTimeZoneReturnsCarbonIntensityForecastTimezoneWhenAvailable() {
        mockResponseWhenRequestingAreaCode("DE", GERMANY_2024_07_11);

        carbonAwareJobManager.updateCarbonIntensityForecast();

        assertThat(carbonAwareJobManager.getTimeZone()).isEqualTo(ZoneId.of("Europe/Berlin"));
    }


    @Test
    void testMoveToNextStateThrowsAnExceptionIfGivenJobsThatAreNotCarbonAwaiting() {
        Job job = anEnqueuedJob().build();

        assertThatCode(() -> carbonAwareJobManager.moveToNextState(job))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only jobs in CarbonAwaitingState can move to a next state");
    }

    @Test
    void testMoveToNextStateEnqueuesCarbonAwaitingJobsThatHaveDeadlineInThePast() {
        mockResponseWhenRequestingAreaCode("BE", BELGIUM_2024_07_11);
        carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
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
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
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
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
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
            carbonAwareJobManager = new CarbonAwareJobManager(setupCarbonAwareConfiguration("BE"), jsonMapper);
            carbonAwareJobManager.updateCarbonIntensityForecast();

            Job job1 = aJob().withCarbonAwareAwaitingState(CarbonAwarePeriod.between(now().plus(1, DAYS), now().plus(1, DAYS).plus(2, HOURS))).build();

            carbonAwareJobManager.moveToNextState(job1);

            assertThat(job1).hasStates(AWAITING);
        }
    }

    private CarbonAwareConfiguration setupCarbonAwareConfiguration(String areaCode) {
        return usingStandardCarbonAwareConfiguration().andCarbonIntensityApiUrl(carbonIntensityApiBaseUrl).andAreaCode(areaCode);
    }

    private Instant startOfDay(LocalDate localDate) {
        return localDate.atStartOfDay(ZoneId.of("Europe/Brussels")).toInstant();
    }
}
