package org.jobrunr.carbonaware;

import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;

public class CarbonAwareJobManagerTest {
    private final CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(usingStandardCarbonAwareConfiguration(), new JacksonJsonMapper());

    @Test
    void testGetDailyRefreshTimeShouldGiveResultBetweenGivenRefreshTimeAndAnHourLater() {
        Instant result = carbonAwareJobManager.getDailyRefreshTime();
        ZonedDateTime expectedTime = ZonedDateTime.now(carbonAwareJobManager.getTimeZone())
                .truncatedTo(HOURS)
                .withHour(19);

        assertThat(result).isAfterOrEqualTo(expectedTime.toInstant());
        assertThat(result).isBefore(expectedTime.plusHours(1).toInstant());
    }

    @Test
    void testGetDailyRefreshTimeShouldGiveTheSameResultOnConsecutiveCalls() {
        Instant firstCall = carbonAwareJobManager.getDailyRefreshTime();
        Instant secondCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    void testDailyRefreshTimeShouldGiveResultsIn5SecondsBuckets() {
        Instant firstCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall.getEpochSecond() % 5).isZero();
    }

}
