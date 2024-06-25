package org.jobrunr.utils.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.carbonaware.DayAheadEnergyPrices;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Instant;

// TODO move this class to the correct place
public class DayAheadEnergyPricesAssert extends AbstractAssert<DayAheadEnergyPricesAssert, DayAheadEnergyPrices> {
    protected DayAheadEnergyPricesAssert(DayAheadEnergyPrices dayAheadEnergyPrices) {
        super(dayAheadEnergyPrices, DayAheadEnergyPricesAssert.class);
    }

    public static DayAheadEnergyPricesAssert assertThat(DayAheadEnergyPrices dayAheadEnergyPrices) {
        return new DayAheadEnergyPricesAssert(dayAheadEnergyPrices);
    }

    public DayAheadEnergyPricesAssert hasAreaCode(String areaCode) {
        Assertions.assertThat(actual.getAreaCode()).isEqualTo(areaCode);
        return this;
    }

    public DayAheadEnergyPricesAssert hasHourlyEnergyPricesSize(int size) {
        Assertions.assertThat(actual.getHourlyEnergyPrices()).hasSize(size);
        return this;
    }

    public DayAheadEnergyPricesAssert hasHourlyEnergyPriceAt(int index, Instant periodStartAt, double price) {
        DayAheadEnergyPrices.HourlyEnergyPrice hourlyEnergyPrice = actual.getHourlyEnergyPrices().get(index);
        Assertions.assertThat(hourlyEnergyPrice.getPeriodStartAt()).isEqualTo(periodStartAt);
        Assertions.assertThat(hourlyEnergyPrice.getPrice()).isEqualTo(price);
        return this;
    }

    public DayAheadEnergyPricesAssert hasValidDataFor(CarbonAwarePeriod carbonAwarePeriod) {
        Assertions.assertThat(actual.hasDataForPeriod(carbonAwarePeriod)).isTrue();
        return this;
    }

    public DayAheadEnergyPricesAssert hasNoValidDataFor(CarbonAwarePeriod carbonAwarePeriod) {
        Assertions.assertThat(actual.hasDataForPeriod(carbonAwarePeriod)).isFalse();
        return this;
    }

    public DayAheadEnergyPricesAssert hasNullUnit() {
        Assertions.assertThat(actual.getUnit()).isNull();
        return this;
    }
}
