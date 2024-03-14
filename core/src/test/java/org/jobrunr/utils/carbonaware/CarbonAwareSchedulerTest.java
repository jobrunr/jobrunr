package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InstantMocker;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.UUID;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.carbonaware.CarbonAwareScheduler.isDayBetweenNowAndEnd;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonAwareSchedulerTest {
    @Mock
    private Job job;
    @Mock
    private DayAheadEnergyPrices dayAheadEnergyPrices;

    @Test
    public void testWaitForSunday_whenSundayDataNotAvailable_andSundayIsIncluded() {
        // Assume
        CarbonAwareScheduler carbonAwareScheduler = new CarbonAwareScheduler(new JacksonJsonMapper());
        CarbonAwareAwaitingState carbonAwareAwaitingState = new CarbonAwareAwaitingState(Instant.now().plusSeconds(60 * 60 * 24 * 7)); // 1 week from now

        // Act
        boolean result = carbonAwareScheduler.waitJobIfDayAvailableAndDataNotAvailable(DayOfWeek.SUNDAY, job, carbonAwareAwaitingState);

        // Assert
        assertThat(result).isTrue(); // Expect to wait since data for Sunday is not available
    }

    @Test
    public void testDoNotWaitForSunday_whenSundayDataNotAvailable_andSundayIsNotIncluded() {
        // Assume
        CarbonAwareScheduler carbonAwareScheduler = new CarbonAwareScheduler(new JacksonJsonMapper());
        try(MockedStatic<Instant> timeNow = InstantMocker.mockTime("2022-12-13T14:00:00Z")){
            CarbonAwareAwaitingState carbonAwareAwaitingState = new CarbonAwareAwaitingState(Instant.parse("2022-12-14T23:00:00Z"));
            when(job.getId()).thenReturn(UUID.randomUUID());
            Instant nextSunday = Instant.parse("2022-12-18T23:00:00Z");
            when(dayAheadEnergyPrices.getMaxHour()).thenReturn(nextSunday.atZone(ZoneId.systemDefault()).toInstant()); // Data is for next Sunday after 15:00

            // Act
            boolean result = carbonAwareScheduler.waitJobIfDayAvailableAndDataNotAvailable(DayOfWeek.SUNDAY, job, carbonAwareAwaitingState);
            assertThat(result).isFalse(); // Expect to schedule job since data for Sunday is not available
        }
    }

    @Test
    void testThatIfDeadlineIsBeforeSunday_ThenWeWaitUntilDayOfDeadline() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    void testThatIfDeadlineIsAfterSunday_ThenWeScheduleJobAtLeastExpensiveHourOnSunday() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    void testThatIfExceptionOccursWhenFetchingDayAheadPrices_JobsAreScheduledImmediatelyWithMessageThatCarbonAwareApiCouldNotBeReached() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    void testWhenFetchingDayAheadPrices_CarbonAwareApiHasNoDataBecauseOfException_JobsAreScheduledImmediatelyWithMessageFromCarbonAwareApi() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    void refreshDayAheadEnergyPricesIfTimeAt2pm() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    void refreshDayAheadEnergyPricesIfTimeAt3pmIfDataIsStillOld() {
        throw new UnsupportedOperationException("test this class");
    }

    @Test
    public void testIsDayBetweenNowAndEnd_testDayInFuture() {
        Instant end = Instant.now().plus(Duration.ofDays(7));
        DayOfWeek futureDay = DayOfWeek.SATURDAY;

        assertThat(isDayBetweenNowAndEnd(end, futureDay)).isTrue();
    }

    @Test
    public void isDayBetweenNowAndEnd_testEndTimeInPast() {
        Instant end = Instant.now().minus(Duration.ofDays(1));
        DayOfWeek anyDay = ZonedDateTime.now(ZoneId.systemDefault()).getDayOfWeek();
        assertThat(isDayBetweenNowAndEnd(end, anyDay)).isFalse();
    }

    @Test
    public void isDayBetweenNowAndEnd_testDayDoesNotOccur() {
        Instant end = Instant.now().plus(Duration.ofDays(2)); // 2 days in the future
        DayOfWeek dayNotOccurring = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(3).getDayOfWeek(); // 3 days from now, beyond end

        assertThat(isDayBetweenNowAndEnd(end, dayNotOccurring)).isFalse();
    }
}