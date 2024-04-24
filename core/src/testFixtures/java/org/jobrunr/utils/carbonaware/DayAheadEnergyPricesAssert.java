package org.jobrunr.utils.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.time.Instant;

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

    public DayAheadEnergyPricesAssert hasHourlyEnergyPriceAt(int index, Instant dateTime, double price) {
        DayAheadEnergyPrices.HourlyEnergyPrice hourlyEnergyPrice = actual.getHourlyEnergyPrices().get(index);
        Assertions.assertThat(hourlyEnergyPrice.getDateTime()).isEqualTo(dateTime);
        Assertions.assertThat(hourlyEnergyPrice.getPrice()).isEqualTo(price);
        return this;
    }

    public DayAheadEnergyPricesAssert hasValidDataFor(CarbonAwarePeriod carbonAwarePeriod) {
        Assertions.assertThat(actual.hasValidData(carbonAwarePeriod)).isTrue();
        return this;
    }

    public DayAheadEnergyPricesAssert hasNoValidDataFor(CarbonAwarePeriod carbonAwarePeriod) {
        Assertions.assertThat(actual.hasValidData(carbonAwarePeriod)).isFalse();
        return this;
    }

    public DayAheadEnergyPricesAssert hasError(String errorMessage) {
        Assertions.assertThat(actual.getAreaCode()).isNull();
        Assertions.assertThat(actual.getHourlyEnergyPrices()).isNull();
        Assertions.assertThat(actual.getIsErrorResponse()).isTrue();
        Assertions.assertThat(actual.getErrorMessage()).isEqualTo(errorMessage);
        return this;
    }
}
