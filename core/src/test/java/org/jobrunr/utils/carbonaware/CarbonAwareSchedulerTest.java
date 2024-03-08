package org.jobrunr.utils.carbonaware;

import org.junit.jupiter.api.Test;

class CarbonAwareSchedulerTest {


    @Test
    void implementMe() {
        throw new UnsupportedOperationException("test this class");
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
}