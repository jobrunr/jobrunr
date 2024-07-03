package org.jobrunr.jobs.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

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

    public DayAheadEnergyPricesAssert hasState(String state) {
        Assertions.assertThat(actual.getState()).isEqualTo(state);
        return this;
    }

    public DayAheadEnergyPricesAssert hasNoData() {
        Assertions.assertThat(actual.hasNoData()).isTrue();
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

    public DayAheadEnergyPricesAssert hasError() {
        Assertions.assertThat(actual.hasError()).isTrue();
        return this;
    }

    public DayAheadEnergyPricesAssert hasErrorCode(String code) {
        Assertions.assertThat(actual.getError().getCode()).isEqualTo(code);
        return this;
    }

    public DayAheadEnergyPricesAssert hasErrorMessage(String message) {
        Assertions.assertThat(actual.getError().getMessage()).isEqualTo(message);
        return this;
    }
}
