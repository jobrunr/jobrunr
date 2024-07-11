package org.jobrunr.server.carbonaware;

import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.ApiResponseStatus;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;
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

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.server.carbonaware.CarbonApiMockResponses.BELGIUM_2024_07_11;

class CarbonIntensityForecastTest {
    @Test
    void canDeserializeCarbonIntensityForecastUsingJackson() {
        JsonMapper jsonMapper = new JacksonJsonMapper();
        CarbonIntensityForecast carbonIntensityForecast = jsonMapper.deserialize(BELGIUM_2024_07_11, CarbonIntensityForecast.class);

        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, Instant.parse("2024-07-10T22:00:00Z"), 16);
    }

    @Test
    void canDeserializeCarbonIntensityForecastUsingJsonB() {
        JsonMapper jsonMapper = new JsonbJsonMapper();
        CarbonIntensityForecast carbonIntensityForecast = jsonMapper.deserialize(BELGIUM_2024_07_11, CarbonIntensityForecast.class);

        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, Instant.parse("2024-07-10T22:00:00Z"), 16)
                .hasForecastEndPeriod(Instant.parse("2024-07-11T22:00:00Z"));
    }

    @Test
    void canDeserializeCarbonIntensityForecastUsingGson() {
        JsonMapper jsonMapper = new GsonJsonMapper();
        CarbonIntensityForecast carbonIntensityForecast = jsonMapper.deserialize(BELGIUM_2024_07_11, CarbonIntensityForecast.class);

        assertThat(carbonIntensityForecast)
                .hasDisplayName("Belgium")
                .hasIntensityForecastSize(24)
                .hasIntensityForecastAt(0, Instant.parse("2024-07-10T22:00:00Z"), 16)
                .hasForecastEndPeriod(Instant.parse("2024-07-11T22:00:00Z"));
    }

    @Test
    void lowestCarbonIntensityInstantReturnsTheLowestCarbonIntensityInstantBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        Instant expected = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(expected, expected.plus(1, HOURS), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        try (var ignored = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(expected);
            assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(expected);
        }
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNowIfCurrentHourIsLowestCarbonIntensityInstant() {
        // GIVEN
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>();
        Instant now = Instant.now();
        Instant to = now.plus(6, HOURS);
        for (int i = 0; i < 6; i++) {
            Instant hour = now.plus(i, HOURS);
            // Make the current hour the cheapest, increase the rank with each subsequent hour
            intensityForecast.add(new TimestampedCarbonIntensityForecast(hour, now.plus(i + 1, HOURS), i));
        }
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        // WHEN
        Instant result = carbonIntensityForecast.lowestCarbonIntensityInstant(now, to);

        // THEN
        Instant expected = now.truncatedTo(HOURS);
        Instant actual = result.truncatedTo(HOURS);
        assertThat(actual).isEqualTo(expected); // The least expensive hour should be the current hour, truncated to hours for comparison.
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoHoursBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T07:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoHoursAfterFrom() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T06:00:00Z"), Instant.parse("2020-01-01T07:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T07:00:00Z"), Instant.parse("2020-01-01T08:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantWhenFromAndToAreEqual() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T08:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        try (var ignored = InstantMocker.mockTime("2020-01-01T08:00:00Z")) {
            assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoForecastAvailable() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), null);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantIsBetweenFromAndTo() {
        Instant from = Instant.parse("2020-01-01T07:00:00Z");
        Instant to = Instant.parse("2020-01-01T09:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T06:00:00Z"), Instant.parse("2020-01-01T07:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T07:00:00Z"), Instant.parse("2020-01-01T08:00:00Z"), 3),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 2),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 4),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T10:00:00Z"), Instant.parse("2020-01-01T11:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        try (MockedStatic<Instant> ignored = InstantMocker.mockTime("2020-01-01T00:00:00Z")) {
            assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
        }
    }


    @Test
    void lowestCarbonIntensityInstantAfterNow() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T23:00:00Z");
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>(Arrays.asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 1)
        ));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        try (MockedStatic<Instant> ignored = InstantMocker.mockTime("2020-01-01T09:00:00Z")) {
            assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(Instant.parse("2020-01-01T09:00:00Z"));
        }
    }

    @Test
    void hasDataForPeriodReturnsTrueWhenForecastIsAvailable() {
        // Setup
        Instant now = Instant.now();
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>();
        intensityForecast.add(new TimestampedCarbonIntensityForecast(now.minusSeconds(3600), now.plusSeconds(3600), 0));
        intensityForecast.add(new TimestampedCarbonIntensityForecast(now.plusSeconds(3600), now.plusSeconds(3600 * 2), 1)); // This price is within the period
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(7200)); // 2 hours ahead

        // Act & Assert
        assertThat(carbonIntensityForecast)
                .hasForecastForPeriod(validPeriod.getFrom(), validPeriod.getTo())
                .hasForecastEndPeriod(now.plusSeconds(3600 * 2));
    }

    @Test
    void hasForecastForPeriodReturnsFalseWhenNoHourInsidePeriod() {
        // Setup
        Instant now = Instant.now();
        ArrayList<TimestampedCarbonIntensityForecast> intensityForecast = new ArrayList<>();
        intensityForecast.add(new TimestampedCarbonIntensityForecast(now.minusSeconds(3600), now.plusSeconds(3600), 0));
        intensityForecast.add(new TimestampedCarbonIntensityForecast(now.plusSeconds(3600), now.plusSeconds(3600 * 2), 1));
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), intensityForecast);

        CarbonAwarePeriod invalidPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(1800)); // 30 minutes ahead

        // Act & Assert
        assertThat(carbonIntensityForecast)
                .hasNoForecastForPeriod(invalidPeriod.getFrom(), invalidPeriod.getTo())
                .hasForecastEndPeriod(now.plusSeconds(3600 * 2));
    }

    @Test
    void hasForecastForPeriodReturnsFalseWhenNoForecastForPeriodIsAvailable() {
        // Setup
        Instant now = Instant.now();
        CarbonIntensityForecast carbonIntensityForecast = new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin", Instant.now().plus(1, DAYS), null);

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(carbonIntensityForecast)
                .hasNoForecastForPeriod(validPeriod.getFrom(), validPeriod.getTo())
                .hasForecastEndPeriod(null);
    }

    @Test
    void hasForecastForPeriodReturnFalseWhenIntensityForecastIsNotSet() {
        // Setup
        Instant now = Instant.now();
        CarbonIntensityForecast carbonIntensityForecastData = new CarbonIntensityForecast();

        CarbonAwarePeriod validPeriod = CarbonAwarePeriod.between(now, now.plusSeconds(3600)); // 1 hour ahead

        // Act & Assert
        assertThat(carbonIntensityForecastData)
                .hasNoForecastForPeriod(validPeriod.getFrom(), validPeriod.getTo())
                .hasForecastEndPeriod(null);
    }
}