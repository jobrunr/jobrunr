package org.jobrunr.utils.carbonaware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DayAheadEnergyPricesTest {
    @Test
    void dayAheadEnergyPricesPojo_WhenDeserializedFromJson_ThenFieldsAreSetCorrectly() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        JsonMapper jsonMapper = new JacksonJsonMapper(objectMapper);
        DayAheadEnergyPrices prices = jsonMapper.deserialize(CarbonApiMockResponses.BELGIUM_2024_03_12, DayAheadEnergyPrices.class);
        assertEquals(33, prices.getHoursAvailable());
        assertEquals("BE", prices.getArea());
        assertEquals(33, prices.getHourlyEnergyPrices().size());
        DayAheadEnergyPrices.HourlyEnergyPrice cheapestPrice = prices.getHourlyEnergyPrices().get(0);
        assertEquals(Instant.parse("2024-03-12T03:00:00Z"), cheapestPrice.getDateTime());
        assertEquals(64.23, cheapestPrice.getPrice());
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_LeastExpensiveHourReturnsBeforeDeadline() {
        Instant deadline = Instant.parse("2020-01-01T10:00:00Z");
        Instant expected = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(expected, 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Timezone", 24, "unit", hourlyEnergyPrices);
        Instant result = prices.leastExpensiveHour(deadline);
        Assertions.assertEquals(expected, result);
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourReturns_NowIfCurrentHourIsLeastExpensiveHour() {
        // ARRANGE
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>();
        Instant now = Instant.now();
        Instant deadline = now.plus(6, ChronoUnit.HOURS);
        for (int i = 0; i < 6; i++) {
            Instant hour = now.plus(i, ChronoUnit.HOURS);
            // Make the current hour the cheapest, increase price with each subsequent hour
            double price = 10.0 + i * 5;
            int rank = i + 1;
            hourlyEnergyPrices.add(new DayAheadEnergyPrices.HourlyEnergyPrice(hour, price, rank));
        }
        DayAheadEnergyPrices dayAheadEnergyPrices = new DayAheadEnergyPrices("Area", "State", "Timezone", 6, "unit", hourlyEnergyPrices);

        //ACT
        Instant result = dayAheadEnergyPrices.leastExpensiveHour(deadline);

        // ASSERT
        Instant expected = now.truncatedTo(ChronoUnit.HOURS);
        Instant actual = result.truncatedTo(ChronoUnit.HOURS);
        Assertions.assertEquals(expected, actual, "The least expensive hour should be the current hour, truncated to hours for comparison.");
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourThrowsException_IfNoHoursBeforeDeadline() {
        Instant deadline = Instant.parse("2020-01-01T07:00:00Z");
        ArrayList<DayAheadEnergyPrices.HourlyEnergyPrice> hourlyEnergyPrices = new ArrayList<>(Arrays.asList(
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T08:00:00Z"), 10.0, 1),
                new DayAheadEnergyPrices.HourlyEnergyPrice(Instant.parse("2020-01-01T09:00:00Z"), 20.0, 2)
        ));
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State",
                "Timezone", 24, "unit", hourlyEnergyPrices);

        Assertions.assertThrows(IllegalStateException.class, () -> prices.leastExpensiveHour(deadline));
    }

    @Test
    void dayAheadEnergyPrices_LeastExpensiveHourThrowsException_IfNoInformationAvailable() {
        Instant deadline = Instant.parse("2020-01-01T10:00:00Z");
        DayAheadEnergyPrices prices = new DayAheadEnergyPrices("Area", "State", "Timezone", 24, "unit", new ArrayList<>());

        Assertions.assertThrows(IllegalStateException.class, () -> prices.leastExpensiveHour(deadline));
    }

}