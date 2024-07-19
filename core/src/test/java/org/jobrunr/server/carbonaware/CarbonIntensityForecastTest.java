package org.jobrunr.server.carbonaware;

import org.jetbrains.annotations.NotNull;
import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.ApiResponseStatus;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast.TimestampedCarbonIntensityForecast;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.jobrunr.utils.mapper.jsonb.JsonbJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Arrays.asList;
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
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(expected, expected.plus(1, HOURS), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 1)
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(expected);
        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(expected);
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoForecastBeforeDeadline() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T07:00:00Z");
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T07:00:00Z"), Instant.parse("2020-01-01T08:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 1),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 2)
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoForecastAfterFrom() {
        Instant from = Instant.parse("2020-01-01T08:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T06:00:00Z"), Instant.parse("2020-01-01T07:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T07:00:00Z"), Instant.parse("2020-01-01T08:00:00Z"), 1)
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantReturnsNullIfNoForecastAvailable() {
        Instant from = Instant.parse("2020-01-01T00:00:00Z");
        Instant to = Instant.parse("2020-01-01T10:00:00Z");
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(null);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isNull();
    }

    @Test
    void lowestCarbonIntensityInstantIsBetweenFromAndTo() {
        Instant from = Instant.parse("2020-01-01T07:00:00Z");
        Instant to = Instant.parse("2020-01-01T09:00:00Z");
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T06:00:00Z"), Instant.parse("2020-01-01T07:00:00Z"), 0),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T07:00:00Z"), Instant.parse("2020-01-01T08:00:00Z"), 3),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T08:00:00Z"), Instant.parse("2020-01-01T09:00:00Z"), 2),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T09:00:00Z"), Instant.parse("2020-01-01T10:00:00Z"), 4),
                new TimestampedCarbonIntensityForecast(Instant.parse("2020-01-01T10:00:00Z"), Instant.parse("2020-01-01T11:00:00Z"), 1)
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

        assertThat(carbonIntensityForecast.lowestCarbonIntensityInstant(from, to)).isEqualTo(Instant.parse("2020-01-01T08:00:00Z"));
    }

    @Test
    void hasDataForPeriodReturnsTrueWhenForecastIsAvailable() {
        // Setup
        Instant now = Instant.now();
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(now.minusSeconds(3600), now.plusSeconds(3600), 0),
                new TimestampedCarbonIntensityForecast(now.plusSeconds(3600), now.plusSeconds(3600 * 2), 1) // This price is within the period
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

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
        List<TimestampedCarbonIntensityForecast> intensityForecast = asList(
                new TimestampedCarbonIntensityForecast(now.minusSeconds(3600), now.plusSeconds(3600), 0),
                new TimestampedCarbonIntensityForecast(now.plusSeconds(3600), now.plusSeconds(3600 * 2), 1)
        );
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(intensityForecast);

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
        CarbonIntensityForecast carbonIntensityForecast = createCarbonIntensityForecast(null);

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

    private static @NotNull CarbonIntensityForecast createCarbonIntensityForecast(List<TimestampedCarbonIntensityForecast> intensityForecast) {
        return new CarbonIntensityForecast(new ApiResponseStatus("OK", ""), "ENTSO-E", "10Y1001A1001A82H", "Germany", "Europe/Berlin",
                Instant.now().plus(1, DAYS), Duration.ofHours(3), intensityForecast);
    }

}