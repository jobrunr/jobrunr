package org.jobrunr.carbonaware;

import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CarbonAwareManagerTest {
    private final CarbonAwareJobManager carbonAwareJobManager = new CarbonAwareJobManager(CarbonAwareConfiguration.usingStandardCarbonAwareConfiguration(), new JacksonJsonMapper());

    @Test
    void testGetDailyRunTimeShouldGiveResultBetween18And19() {
        Instant result = carbonAwareJobManager.getZookeeperTaskDailyRunTime(18);
        ZonedDateTime expectedTime = ZonedDateTime.now(ZoneId.of("Europe/Brussels"))
                .withHour(18).withMinute(0).withSecond(0);

        assertTrue(result.isAfter(expectedTime.toInstant()) || result.equals(expectedTime.toInstant()));
        assertTrue(result.isBefore(expectedTime.plusHours(1).toInstant()));
    }

    @Test
    void testGetDailyRunTimeShouldNotGive2ConsecutiveEqualResults() {
        Instant firstCall = carbonAwareJobManager.getZookeeperTaskDailyRunTime(18);
        Instant secondCall = carbonAwareJobManager.getZookeeperTaskDailyRunTime(18);
        assertNotEquals(firstCall, secondCall);
    }

    @Test
    void testGetDailyRunTimeShouldGiveResultsIn5SecondsBuckets() {
        Instant firstCall = carbonAwareJobManager.getZookeeperTaskDailyRunTime(18);
        Instant secondCall = carbonAwareJobManager.getZookeeperTaskDailyRunTime(18);
        assertEquals(0, firstCall.getEpochSecond() % 5);
        assertEquals(0, secondCall.getEpochSecond() % 5);
    }

}
