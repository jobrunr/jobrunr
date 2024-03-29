package org.jobrunr.utils.carbonaware;

import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InstantMocker;
import org.mockito.MockedStatic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

import static org.jobrunr.JobRunrAssertions.assertThat;

class DayAheadEnergyPricesTest {
    @Test
    void dayAheadEnergyPricesPojo_WhenDeserializedFromJson_UsingJackson_ThenFieldsAreSetCorrectly() {
        JsonMapper jsonMapper = new JacksonJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(CarbonApiMockResponses.BELGIUM_2024_03_12, DayAheadEnergyPrices.class);
        assertDeserializedDayAheadEnergyPrices(prices);
    }

    @Test
    void dayAheadEnergyPricesPojo_WhenDeserializedFromJson_UsingJsonB_ThenFieldsAreSetCorrectly() {
        JsonMapper jsonMapper = new JsonbJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(CarbonApiMockResponses.BELGIUM_2024_03_12, DayAheadEnergyPrices.class);
        assertDeserializedDayAheadEnergyPrices(prices);
    }

    @Test
    void dayAheadEnergyPricesPojo_WhenDeserializedFromJson_UsingGson_ThenFieldsAreSetCorrectly() {
        JsonMapper jsonMapper = new GsonJsonMapper();
        DayAheadEnergyPrices prices = jsonMapper.deserialize(CarbonApiMockResponses.BELGIUM_2024_03_12, DayAheadEnergyPrices.class);
        assertDeserializedDayAheadEnergyPrices(prices);
    }

    private static void assertDeserializedDayAheadEnergyPrices(DayAheadEnergyPrices prices) {
        assertThat(prices.getArea()).isEqualTo("BE");
        assertThat(prices.getHourlyEnergyPrices().size()).isEqualTo(33);
        DayAheadEnergyPrices.HourlyEnergyPrice cheapestPrice = prices.getHourlyEnergyPrices().get(0);
        assertThat(cheapestPrice.getDateTime()).isEqualTo(Instant.parse("2024-03-12T03:00:00Z"));
        assertThat(cheapestPrice.getPrice()).isEqualTo(64.23);
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_LeastExpensiveHourReturnsBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        Instant expected = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(expected, 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("DE", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);
        try(var a = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            Instant result = prices.leastExpensiveHour(from, to);
            assertThat(result).isEqualTo(expected);
            result = prices.leastExpensiveHour(from, to);
            assertThat(result).isEqualTo(expected);
        }
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturnsNow_IfCurrentHourIsLeastExpensiveHour() {
        // GIVEN
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>();
        Instant now = Instant.now();
        Instant to = now.plus(6, ChronoUnit.HOURS);
        for (int i = 0; i < 6; i++) {
            Instant hour = now.plus(i, ChronoUnit.HOURS);
            // Make the current hour the cheapest, increase price with each subsequent hour
            double price = 10.0 + i * 5;
            int rank = i + 1;
            hourlyEnergyPrices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(hour, price, rank));
        }
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices("Area", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        // WHEN
        Instant result = dayAheadEnergyPrices.leastExpensiveHour(now, to);

        // THEN
        Instant expected = now.truncatedTo(ChronoUnit.HOURS);
        Instant actual = result.truncatedTo(ChronoUnit.HOURS);
        assertThat(actual).isEqualTo(expected); // The least expensive hour should be the current hour, truncated to hours for comparison.
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturnsNull_IfNoHoursBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T07:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State",
                "Europe/Berlin", "unit", hourlyEnergyPrices);

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void leastExpensiveHourReturnsNull_IfNoHoursAfterFrom() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T06:00:00Z"), 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T07:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void leastExpensiveHourReturnsHour_whenFromAndToAreEqual() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        try(var a = InstantMocker.mockTime("2020-01-01T08:00:00Z")) {
            DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturnsNull_IfNoInformationAvailable() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Europe/Berlin", "unit", new ArrayList<>());

        assertThat(prices.leastExpensiveHour(from, to)).isNull();
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourShouldReturnHourAfterFromAndBeforeTo() {
        Instant from = Instant.parse("2020-01-01T07:00:00Z");
        Instant to = Instant.parse("2020-01-01T09:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T06:00:00Z"), 2.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T10:00:00Z"), 4.0, 2),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 8.0, 3),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T07:00:00Z"), 15.0, 4),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 5)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Europe/Berlin", "unit", hourlyEnergyPrices);

        try(MockedStatic<Instant> a = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }


    @Test
    void dayAheadEnergyPrices_LeastExpensiveHour_shouldReturnHourAfterNow() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T23:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 8.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("DE", null, "Europe/Berlin", "EUR/MWH", hourlyEnergyPrices);

        try(MockedStatic<Instant> a = InstantMocker.mockTime("2020-01-01T09:00:00Z")) {
            assertThat(prices.leastExpensiveHour(from, to)).isEqualTo(Instant.parse("2020-01-01T09:00:00Z"));
        }
    }

    @Test
    void hasValidData_whenValid_returnsTrue() {
        // Setup
        Instant now = Instant.now();
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> prices = new ArrayList<>();
        prices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(now.minusSeconds(3600), 10.0, 1));
        prices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(now.plusSeconds(3600), 20.0, 2)); // This price is within the period
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("Area", null, "Europe/Berlin", "Unit", prices);
        pricesData.setIsErrorResponse(false);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(7200)); // 2 hours ahead

        // Act & Assert
        assertThat(pricesData.hasValidData(validPeriod)).isTrue();
    }

    @Test
    void hasValidData_whenNoHourInsidePeriod_returnsFalse() {
        // Setup
        Instant now = Instant.now();
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> prices = new ArrayList<>();
        prices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(now.minusSeconds(3600), 10.0, 1));
        prices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(now.plusSeconds(3600), 20.0, 2));
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("BE", "EE", "Europe/Brussels", "Unit", prices);
        pricesData.setIsErrorResponse(false);

        CarbonAwarePeriod invalidPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(1800)); // 30 minutes ahead

        // Act & Assert
        assertThat(pricesData.hasValidData(invalidPeriod)).isFalse();
    }

    @Test
    void hasValidData_whenNoDataAvailable_returnsFalse() {
        // Setup
        Instant now = Instant.now();
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices("BE", null, "Europe/Brussels", "EUR/MWH", new ArrayList<>());
        pricesData.setIsErrorResponse(false);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(pricesData.hasValidData(validPeriod)).isFalse();
    }

    @Test
    void hasValidData_whenErrorResponse_returnsFalse() {
        // Setup
        Instant now = Instant.now();
        DayAheadEnergyPrices pricesData = new DayAheadEnergyPrices(null, null, null, null, null);
        pricesData.setIsErrorResponse(true);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(pricesData.hasValidData(validPeriod)).isFalse();
    }
}