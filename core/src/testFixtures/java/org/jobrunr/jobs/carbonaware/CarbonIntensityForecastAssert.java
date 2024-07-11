package org.jobrunr.jobs.carbonaware;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;

import java.time.Instant;

public class CarbonIntensityForecastAssert extends AbstractAssert<CarbonIntensityForecastAssert, CarbonIntensityForecast> {
    protected CarbonIntensityForecastAssert(CarbonIntensityForecast carbonIntensityForecast) {
        super(carbonIntensityForecast, CarbonIntensityForecastAssert.class);
    }

    public static CarbonIntensityForecastAssert assertThat(CarbonIntensityForecast carbonIntensityForecast) {
        return new CarbonIntensityForecastAssert(carbonIntensityForecast);
    }

    public CarbonIntensityForecastAssert hasDisplayName(String displayName) {
        Assertions.assertThat(actual.getDisplayName()).isEqualTo(displayName);
        return this;
    }

    public CarbonIntensityForecastAssert hasNoForecast() {
        Assertions.assertThat(actual.hasNoForecast()).isTrue();
        return this;
    }

    public CarbonIntensityForecastAssert hasForecastEndPeriod(Instant forecastEndAt) {
        Assertions.assertThat(actual.getForecastEndPeriod()).isEqualTo(forecastEndAt);
        return this;
    }

    public CarbonIntensityForecastAssert hasIntensityForecastSize(int size) {
        Assertions.assertThat(actual.getIntensityForecast()).hasSize(size);
        return this;
    }

    public CarbonIntensityForecastAssert hasIntensityForecastAt(int index, Instant periodStartAt, int rank) {
        TimestampedCarbonIntensityForecast hourlyEnergyPrice = actual.getIntensityForecast().get(index);
        Assertions.assertThat(hourlyEnergyPrice.getPeriodStartAt()).isEqualTo(periodStartAt);
        Assertions.assertThat(hourlyEnergyPrice.getRank()).isEqualTo(rank);
        return this;
    }

    public CarbonIntensityForecastAssert hasIntensityForecastAt(int index, Instant periodStartAt, Instant periodEndAt, int rank) {
        TimestampedCarbonIntensityForecast hourlyEnergyPrice = actual.getIntensityForecast().get(index);
        Assertions.assertThat(hourlyEnergyPrice.getPeriodStartAt()).isEqualTo(periodStartAt);
        Assertions.assertThat(hourlyEnergyPrice.getPeriodEndAt()).isEqualTo(periodEndAt);
        Assertions.assertThat(hourlyEnergyPrice.getRank()).isEqualTo(rank);
        return this;
    }

    public CarbonIntensityForecastAssert hasForecastForPeriod(Instant periodStartAt, Instant periodEndAt) {
        Assertions.assertThat(actual.hasForecastForPeriod(periodStartAt, periodEndAt)).isTrue();
        return this;
    }

    public CarbonIntensityForecastAssert hasNoForecastForPeriod(Instant periodStartAt, Instant periodEndAt) {
        Assertions.assertThat(actual.hasNoForecastForPeriod(periodStartAt, periodEndAt)).isTrue();
        return this;
    }

    public CarbonIntensityForecastAssert hasError() {
        Assertions.assertThat(actual.hasError()).isTrue();
        return this;
    }

    public CarbonIntensityForecastAssert hasErrorCode(String code) {
        Assertions.assertThat(actual.getApiResponseStatus().getCode()).isEqualTo(code);
        return this;
    }

    public CarbonIntensityForecastAssert hasErrorMessage(String message) {
        Assertions.assertThat(actual.getApiResponseStatus().getMessage()).isEqualTo(message);
        return this;
    }
}
