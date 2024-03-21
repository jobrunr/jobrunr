package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CarbonAwareSchedulerTest {
    @Mock
    private Job job;
    @Mock
    private DayAheadEnergyPrices dayAheadEnergyPrices;

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