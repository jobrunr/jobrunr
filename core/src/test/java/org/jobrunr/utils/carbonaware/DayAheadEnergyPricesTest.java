package org.jobrunr.utils.carbonaware;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

class DayAheadEnergyPricesTest {

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_LeastExpensiveHourReturnsBeforeDeadline() {
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices();


        Instant deadline = Instant.now().plus(6, ChronoUnit.HOURS);
        dayAheadEnergyPrices.leastExpensiveHour(deadline);

        throw new UnsupportedOperationException("Finish me, add data to dayAheadEnergyPrice");
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_NowIfCurrentHourIsLeastExpensiveHour() {
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices();


        Instant deadline = Instant.now().plus(6, ChronoUnit.HOURS);
        dayAheadEnergyPrices.leastExpensiveHour(deadline);

        throw new UnsupportedOperationException("Finish me, add data to dayAheadEnergyPrice");
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_NullIfNoInformationAvailable() {
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices();


        Instant deadline = Instant.now().plus(3, ChronoUnit.DAYS);
        dayAheadEnergyPrices.leastExpensiveHour(deadline);

        throw new UnsupportedOperationException("Finish me, add data to dayAheadEnergyPrice");
    }

}