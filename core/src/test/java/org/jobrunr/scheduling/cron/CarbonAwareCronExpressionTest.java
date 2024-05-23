package org.jobrunr.scheduling.cron;

import org.jobrunr.scheduling.RecurringJobNextRun;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.jobrunr.scheduling.cron.CarbonAwareCronExpression.create;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CarbonAwareCronExpressionTest {
    @Test
    void testCreateWithValidInput() {
        assertDoesNotThrow(() -> create("0 0 * * * PT2H PT4H"));
    }

    @Test
    void testCreateCronWithSeconds() {
        assertDoesNotThrow(() -> create("2 0 0 * * * PT2H PT4H"));
    }

    @Test
    void testCreateWithInvalidInput_missing1Duration() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("0 0 * * * PT2H"));
    }

    @Test
    void testInvalidDurationFormat() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("0 0 * * * PT2H PT999"));
    }

    @Test
    void testMinimumTotalDurationRequirement3Hours() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("0 0 * * * PT1H PT1H"));
    }

    @Test
    void testDurationBetweenRuns_every15Minutes_throwsException() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("*/15 * * * * PT3H PT3H"));
    }

    @Test
    void testDurationBetweenRuns_every2HoursAnd59MinutesAnd59Seconds_throwsException() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("0 */2 * * * PT0S PT2H59M59S"));
    }

//    @Test
//    void testNextRunTimeCalculation() {
//        String cronExpression = "0 0 * * *";
//        Duration before = Duration.ofHours(2);
//        Duration after = Duration.ofHours(2);
//        CarbonAwareCronExpression expression = CarbonAwareCronExpression.create(cronExpression, before, after);
//        Instant baseTime = Instant.now();
//        ZoneId zoneId = ZoneId.systemDefault();
//        RecurringJobNextRun nextRun = expression.next(baseTime, baseTime, zoneId);
//
//        Instant expectedEarliestStart = baseTime.plus(2, HOURS);
//        Instant expectedLatestStart = baseTime.plus(4, HOURS);
//
//        assertEquals(truncatedToSeconds(expectedEarliestStart), nextRun.getCarbonAwarePeriod().getTo());
//        assertEquals(truncatedToSeconds(expectedLatestStart), nextRun.getCarbonAwarePeriod().getFrom());
//    }
//
//    private static Instant truncatedToSeconds(Instant instant) {
//        return instant.truncatedTo(SECONDS);
//    }


    @Test
    void testBoundaryConditionsForDurations() {
        assertThrows(InvalidCarbonAwareCronExpressionException.class,
                () -> create("0 0 * * * PT1H59M59S PT1H"));

        assertDoesNotThrow(() -> create("0 0 * * * PT2H PT1H"));
    }

    @Test
    void testTimeZoneImpactOnScheduling() {
        String expression = "0 0 * * * PT2H PT2H";
        CarbonAwareCronExpression carbonExpr = CarbonAwareCronExpression.create(expression);
        ZoneId gmtZone = ZoneId.of("GMT");
        ZoneId pstZone = ZoneId.of("America/Los_Angeles");

        Instant baseTime = Instant.parse("2024-01-01T00:00:00Z");
        RecurringJobNextRun gmtRun = carbonExpr.next(baseTime, baseTime, gmtZone);
        RecurringJobNextRun pstRun = carbonExpr.next(baseTime, baseTime, pstZone);

        assertNotEquals(gmtRun.getCarbonAwarePeriod().getFrom(), pstRun.getCarbonAwarePeriod().getTo());
    }

}