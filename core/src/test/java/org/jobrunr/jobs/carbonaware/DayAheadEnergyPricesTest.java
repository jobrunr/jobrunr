package org.jobrunr.jobs.carbonaware;

import org.jobrunr.jobs.carbonaware.DayAheadEnergyPrices.HourlyEnergyPrice;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.carbonaware.CarbonApiMockResponses.BELGIUM_2024_03_12;

class DayAheadEnergyPricesTest {
    @Test
    void canDeserializeDayAheadEnergyPricesUsingJackson() {
        JsonMapper jsonMapper = new JacksonJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(BELGIUM_2024_03_12, DayAheadEnergyPrices.class);

        assertThat(prices.getAreaCode()).isEqualTo("BE");
        assertThat(prices.getHourlyEnergyPrices().size()).isEqualTo(33);
        HourlyEnergyPrice cheapestPrice = prices.getHourlyEnergyPrices().get(0);
        assertThat(cheapestPrice.getPeriodStartAt()).isEqualTo(Instant.parse("2024-03-12T03:00:00Z"));
        assertThat(cheapestPrice.getPrice()).isEqualTo(64.23);
    }

    @Test
    void canDeserializeDayAheadEnergyPricesUsingJsonB() {
        JsonMapper jsonMapper = new JsonbJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(BELGIUM_2024_03_12, DayAheadEnergyPrices.class);

        assertThat(prices.getAreaCode()).isEqualTo("BE");
        assertThat(prices.getHourlyEnergyPrices().size()).isEqualTo(33);
        HourlyEnergyPrice cheapestPrice = prices.getHourlyEnergyPrices().get(0);
        assertThat(cheapestPrice.getPeriodStartAt()).isEqualTo(Instant.parse("2024-03-12T03:00:00Z"));
        assertThat(cheapestPrice.getPrice()).isEqualTo(64.23);
    }

    @Test
    void canDeserializeDayAheadEnergyPricesUsingGson() {
        JsonMapper jsonMapper = new GsonJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(BELGIUM_2024_03_12, DayAheadEnergyPrices.class);

        assertThat(prices.getAreaCode()).isEqualTo("BE");
        assertThat(prices.getHourlyEnergyPrices().size()).isEqualTo(33);
        HourlyEnergyPrice cheapestPrice = prices.getHourlyEnergyPrices().get(0);
        assertThat(cheapestPrice.getPeriodStartAt()).isEqualTo(Instant.parse("2024-03-12T03:00:00Z"));
        assertThat(cheapestPrice.getPrice()).isEqualTo(64.23);
    }

    @Test
    void leastExpensiveHourReturnsTheLeastExpensiveHourBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        Instant expected = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(expected, 10.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("DE", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        try (var ignored = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            Instant result = prices.leastExpensiveHour(from, to);
            assertThat(result).isEqualTo(expected);
            result = prices.leastExpensiveHour(from, to);
            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    void leastExpensiveHourReturnsNowIfCurrentHourIsLeastExpensiveHour() {
        // GIVEN
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>();
        Instant now = Instant.now();
        Instant to = now.plus(6, HOURS);
        for (int i = 0; i < 6; i++) {
            Instant hour = now.plus(i, HOURS);
            // Make the current hour the cheapest, increase price with each subsequent hour
            double price = 10.0 + i * 5;
            int rank = i + 1;
            hourlyEnergyPrices.add(new HourlyEnergyPrice(hour, price, rank));
        }
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices("AreaCode", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        // WHEN
        Instant result = dayAheadEnergyPrices.leastExpensiveHour(now, to);

        // THEN
        Instant expected = now.truncatedTo(HOURS);
        Instant actual = result.truncatedTo(HOURS);
        assertThat(actual).isEqualTo(expected); // The least expensive hour should be the current hour, truncated to hours for comparison.
    }

    @Test
    void leastExpensiveHourReturnsNullIfNoHoursBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T07:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 10.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("AreaCode", "State",
                "Europe/Berlin", "unit", hourlyEnergyPrices);

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void leastExpensiveHourReturnsNullIfNoHoursAfterFrom() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(Instant.parse("2020-01-01T06:00:00Z"), 10.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T07:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("AreaCode", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void leastExpensiveHourReturnsHourWhenFromAndToAreEqual() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 10.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        try (var ignored = InstantMocker.mockTime("2020-01-01T08:00:00Z")) {
            DayAheadEnergyPrices prices = new DayAheadEnergyPrices("AreaCode", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }

    @Test
    void leastExpensiveHourReturnsNullIfNoInformationAvailable() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("AreaCode", "State", "Europe/Berlin", "unit", new ArrayList<>());

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void leastExpensiveHourShouldReturnHourAfterFromAndBeforeTo() {
        Instant from = Instant.parse("2020-01-01T07:00:00Z");
        Instant to = Instant.parse("2020-01-01T09:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(Instant.parse("2020-01-01T06:00:00Z"), 2.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T10:00:00Z"), 4.0, 2),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 8.0, 3),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T07:00:00Z"), 15.0, 4),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 5)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("AreaCode", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        try (MockedStatic<Instant> a = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }


    @Test
    void leastExpensiveHourShouldReturnHourAfterNow() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T23:00:00Z");
        ArrayList<HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 8.0, 1),
                new HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("DE", null, "Europe/Berlin", "EUR/MWH", hourlyEnergyPrices);

        try (MockedStatic<Instant> a = InstantMocker.mockTime("2020-01-01T09:00:00Z")) {
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T09:00:00Z"));
        }
    }

    @Test
    void hasDataForPeriodReturnsTrueWhenDataIsAvailable() {
        // Setup
        Instant now = Instant.now();
        ArrayList<HourlyEnergyPrice> prices = new ArrayList<>();
        prices.add(new HourlyEnergyPrice(now.minusSeconds(3600), 10.0, 1));
        prices.add(new HourlyEnergyPrice(now.plusSeconds(3600), 20.0, 2)); // This price is within the period
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("AreaCode", null, "Europe/Berlin", "Unit", prices);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(7200)); // 2 hours ahead

        // Act & Assert
        assertThat(pricesData.hasDataForPeriod(validPeriod)).isTrue();
    }

    @Test
    void hasDataForPeriodReturnsFalseWhenNoHourInsidePeriod() {
        // Setup
        Instant now = Instant.now();
        ArrayList<HourlyEnergyPrice> prices = new ArrayList<>();
        prices.add(new HourlyEnergyPrice(now.minusSeconds(3600), 10.0, 1));
        prices.add(new HourlyEnergyPrice(now.plusSeconds(3600), 20.0, 2));
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("BE", "EE", "Europe/Brussels", "Unit", prices);

        CarbonAwarePeriod invalidPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(1800)); // 30 minutes ahead

        // Act & Assert
        assertThat(pricesData.hasDataForPeriod(invalidPeriod)).isFalse();
    }

    @Test
    void hasDataForPeriodReturnsFalseWhenNoDataForPeriodAvailable() {
        // Setup
        Instant now = Instant.now();
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("BE", null, "Europe/Brussels", "EUR/MWH", new ArrayList<>());

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(pricesData.hasDataForPeriod(validPeriod)).isFalse();
    }

    @Test
    void hasDataReturnFalseWhenHourlyPricesAreNotSet() {
        // Setup
        Instant now = Instant.now();
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices(null, null, null, null, null);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(pricesData.hasDataForPeriod(validPeriod)).isFalse();
    }
}