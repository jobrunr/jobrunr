package org.jobrunr.jobs.carbonaware;

import org.jobrunr.jobs.carbonaware.CarbonAwareJobManager;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.carbonaware.CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration;

public class CarbonAwareJobManagerTest {
    private final CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(usingStandardCarbonAwareConfiguration(), new JacksonJsonMapper());

    @Test
    void testGetDailyRefreshTimeShouldGiveResultBetweenGivenRefreshTimeAndAnHourLater() {
        ZonedDateTime result = carbonAwareJobManager.getDailyRefreshTime();
        ZonedDateTime expectedTime = ZonedDateTime.now(carbonAwareJobManager.getTimeZone())
                .truncatedTo(HOURS)
                .withHour(18);

        assertThat(result).isAfterOrEqualTo(expectedTime);
        assertThat(result).isBefore(expectedTime.plusHours(1));
    }

    @Test
    void testGetDailyRefreshTimeShouldGiveTheSameResultOnConsecutiveCalls() {
        ZonedDateTime firstCall = carbonAwareJobManager.getDailyRefreshTime();
        ZonedDateTime secondCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    void testDailyRefreshTimeShouldGiveResultsIn5SecondsBuckets() {
        ZonedDateTime firstCall = carbonAwareJobManager.getDailyRefreshTime();

        assertThat(firstCall.toInstant().getEpochSecond() % 5).isZero();
    }

}
