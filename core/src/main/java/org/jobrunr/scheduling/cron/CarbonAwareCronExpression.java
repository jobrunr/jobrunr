package org.jobrunr.scheduling.cron;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.Schedule;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.carbonaware.CarbonAwareJobManager;
import org.jobrunr.utils.carbonaware.CarbonAwarePeriod;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

@Beta(note = "WIP")
public class CarbonAwareCronExpression extends Schedule {

    private final CronExpression cronExpression;
    private final Duration allowedDurationBefore; //format: PnDTnHnMnS
    private final Duration allowedDurationAfter;
    private final CarbonAwareJobManager carbonAwareJobManager;

    public CarbonAwareCronExpression(CronExpression cronExpression, Duration allowedDurationBefore, Duration allowedDurationAfter) {
        this.cronExpression = cronExpression;
        this.allowedDurationBefore = allowedDurationBefore;
        this.allowedDurationAfter = allowedDurationAfter;
        this.carbonAwareJobManager = JobRunr.getBackgroundJobServer().getCarbonAwareJobManager();
    }

    public static CarbonAwareCronExpression create(String expression) {
        String[] fields = expression.trim().toLowerCase().split("\\s+");
        validate(expression, fields);
        String cronExpressionStr = String.join(" ", fields).substring(0, fields.length - 2);
        return new CarbonAwareCronExpression(CronExpression.create(cronExpressionStr), Duration.parse(fields[fields.length - 2]), Duration.parse(fields[fields.length - 1]));
    }

    /**
     * Calculates the next occurrence based on provided base time.
     *
     * @param createdAtInstant Instant object based on which calculating the next occurrence.
     * @return Instant of the next occurrence.
     */
    @Override
    //TODO: WIP review this method
    public Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId) {
        Instant nextPossibleTime = cronExpression.next(createdAtInstant, currentInstant, zoneId);
        if (Duration.between(currentInstant, nextPossibleTime).toDays() > 1) { //why: we only have day-ahead prices
            return nextPossibleTime;
        }

        Instant earliestStart = nextPossibleTime.minus(allowedDurationBefore);
        Instant latestStart = nextPossibleTime.plus(allowedDurationAfter);

        CarbonAwarePeriod carbonAwarePeriod = CarbonAwarePeriod.between(earliestStart, latestStart);
        Instant idealMoment = carbonAwareJobManager.getLeastExpensiveHour(carbonAwarePeriod);
        if (idealMoment == null) {
            return next(createdAtInstant, nextPossibleTime, zoneId);
        }
        return idealMoment;
    }

    private static void validate(String expression, String[] fields) {
        if (expression.isEmpty()) {
            throw new InvalidCarbonAwareCronExpressionException("Empty carbon aware cron expression");
        }
        int count = fields.length;
        if (count > 8 || count < 7) {
            throw new InvalidCarbonAwareCronExpressionException(
                    "Carbon aware crontab expression should have 8 fields (seconds resolution) or 7 fields (minutes resolution).\n" +
                            "Also, last 2 fields has to be parseable as java.time.Duration objects (ISO-8601 PnDTnHnMn.nS format)\n" +
                            "Example:\n " +
                            "`0 0 * * * PT2H PT6H`: run every day at midnight. Job can be scheduled 2 hours before and 6 hours after midnight, in the time when carbon emissions are the lowest.");
        }
        try {
            Duration.parse(fields[count - 2]);
            Duration.parse(fields[count - 1]);
        } catch (Exception e) {
            throw new InvalidCarbonAwareCronExpressionException("Last 2 fields has to be parseable as java.time.Duration objects (ISO-8601 PnDTnHnMn.nS format)" +
                    "Provided: " + fields[count - 2] + " and " + fields[count - 1] + "\n" +
                    "Valid Duration Examples:\n " +
                    "- PT20.345S represents 20.345 seconds, " +
                    "- PT15M represents 15 minutes, " +
                    "- PT10H represents 10 hours, " +
                    "- P2D represents 2 days, " +
                    "- P2DT3H4M represents 2 days, 3 hours and 4 minutes");
        }
    }
}
