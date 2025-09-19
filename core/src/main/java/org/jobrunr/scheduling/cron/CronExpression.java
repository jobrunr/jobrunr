package org.jobrunr.scheduling.cron;

import org.jobrunr.scheduling.Schedule;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.BitSet;

/**
 * Schedule class represents a parsed crontab expression.
 * <p>
 * Original version <a href="https://github.com/asahaf/javacron">https://github.com/asahaf/javacron</a>
 *
 * @author Ahmed AlSahaf
 * @author Ronald Dehuysser (minor modifications)
 */
public class CronExpression extends Schedule {

    private enum DaysAndDaysOfWeekRelation {
        INTERSECT, UNION
    }

    private static final CronFieldParser SECONDS_FIELD_PARSER = new CronFieldParser(CronFieldType.SECOND);

    private static final CronFieldParser MINUTES_FIELD_PARSER = new CronFieldParser(CronFieldType.MINUTE);
    private static final CronFieldParser HOURS_FIELD_PARSER = new CronFieldParser(CronFieldType.HOUR);
    private static final CronFieldParser DAYS_FIELD_PARSER = new CronFieldParser(CronFieldType.DAY);
    private static final CronFieldParser MONTHS_FIELD_PARSER = new CronFieldParser(CronFieldType.MONTH);
    private static final CronFieldParser DAY_OF_WEEK_FIELD_PARSER = new CronFieldParser(CronFieldType.DAY_OF_WEEK);

    private final boolean hasSecondsField;
    private final DaysAndDaysOfWeekRelation daysAndDaysOfWeekRelation;
    private final BitSet seconds;
    private final BitSet minutes;
    private final BitSet hours;
    private final BitSet days;
    private final BitSet months;
    private final BitSet daysOfWeek;
    private final BitSet daysOf5Weeks;
    private final boolean isLastDayOfMonth;
    private boolean isSpecificLastDayOfMonth;

    /**
     * Parses crontab expression, with optional carbon aware margin, and create a Schedule object representing that
     * expression.
     * <p>
     * The expression string can be 5 fields expression for minutes resolution.
     *
     * <pre>
     *  ┌───────────── minute (0 - 59)
     *  │ ┌───────────── hour (0 - 23)
     *  │ │ ┌───────────── day of the month (1 - 31) or L for last day of the month
     *  │ │ │ ┌───────────── month (1 - 12 or Jan/January - Dec/December)
     *  │ │ │ │ ┌───────────── day of the week (0 - 6 or Sun/Sunday - Sat/Saturday)
     *  │ │ │ │ │
     *  │ │ │ │ │
     *  │ │ │ │ │
     * "* * * * *"
     * </pre>
     * <p>
     * or 6 fields expression for higher, seconds resolution.
     *
     * <pre>
     *  ┌───────────── second (0 - 59)
     *  │ ┌───────────── minute (0 - 59)
     *  │ │ ┌───────────── hour (0 - 23)
     *  │ │ │ ┌───────────── day of the month (1 - 31) or L for last day of the month
     *  │ │ │ │ ┌───────────── month (1 - 12 or Jan/January - Dec/December)
     *  │ │ │ │ │ ┌───────────── day of the week (0 - 6 or Sun/Sunday - Sat/Saturday)
     *  │ │ │ │ │ │
     *  │ │ │ │ │ │
     *  │ │ │ │ │ │
     * "* * * * * *"
     * </pre>
     *
     * @param expression a crontab expression string, with optional carbon aware margin, used to create Schedule.
     * @throws InvalidCronExpressionException if the provided crontab expression is
     *                                        invalid. The crontab expression is
     *                                        considered invalid if it is not properly
     *                                        formed, like empty string or contains less
     *                                        than 5 fields or more than 6 field. It's
     *                                        also invalid if the values in a field are
     *                                        beyond the allowed values range of that
     *                                        field. Non-occurring schedules like "0 0
     *                                        30 2 *" is considered invalid too, as Feb
     *                                        never has 30 days and a schedule like this
     *                                        never occurs.
     */
    public CronExpression(String expression) {
        super(expression);
        if (getExpression().isEmpty()) {
            throw new InvalidCronExpressionException("Empty cron expression");
        }
        String[] fields = getExpression().trim().toLowerCase().split("\\s+");
        int count = fields.length;
        if (count > 6 || count < 5) {
            throw new InvalidCronExpressionException(
                    "crontab expression should have 6 fields for (seconds resolution) or 5 fields for (minutes resolution). Provided: " + expression);
        }
        this.hasSecondsField = count == 6;
        String token;
        int index = 0;
        if (this.hasSecondsField) {
            token = fields[index++];
            this.seconds = CronExpression.SECONDS_FIELD_PARSER.parse(token);
        } else {
            this.seconds = new BitSet(1);
            this.seconds.set(0);
        }
        token = fields[index++];
        this.minutes = CronExpression.MINUTES_FIELD_PARSER.parse(token);

        token = fields[index++];
        this.hours = CronExpression.HOURS_FIELD_PARSER.parse(token);

        token = fields[index++];
        String daysToken = token;
        this.days = CronExpression.DAYS_FIELD_PARSER.parse(token);
        this.isLastDayOfMonth = token.equals("l");
        boolean daysStartWithAsterisk = token.startsWith("*");

        token = fields[index++];
        this.months = CronExpression.MONTHS_FIELD_PARSER.parse(token);

        token = fields[index++];
        this.daysOfWeek = CronExpression.DAY_OF_WEEK_FIELD_PARSER.parse(token);
        boolean daysOfWeekStartAsterisk = token.startsWith("*");

        if (token.length() == 2 && token.endsWith("l")) {
            if (this.isLastDayOfMonth) {
                throw new InvalidCronExpressionException("You can only specify the last day of month week in either the DAY field or in the DAY_OF_WEEK field, not both.");
            }
            if (!daysToken.equalsIgnoreCase("*")) {
                throw new InvalidCronExpressionException("when last days of month is specified. the day of the month must be \"*\"");
            }
            // this flag will be used later for finding the next schedule as some months have less than 31 days
            this.isSpecificLastDayOfMonth = true;
        }
        this.daysOf5Weeks = generateDaysOf5Weeks(this.daysOfWeek);

        this.daysAndDaysOfWeekRelation = (daysStartWithAsterisk || daysOfWeekStartAsterisk)
                ? DaysAndDaysOfWeekRelation.INTERSECT
                : DaysAndDaysOfWeekRelation.UNION;

        if (!this.canScheduleActuallyOccur())
            throw new InvalidCronExpressionException("Cron expression not valid. The specified months do not have the day 30th or the day 31st");
    }

    /**
     * Calculates the next occurrence based on provided base time.
     *
     * @param createdAtInstant Instant object based on which calculating the next occurrence.
     * @return Instant of the next occurrence.
     */
    @Override
    public Instant next(Instant createdAtInstant, Instant currentInstant, ZoneId zoneId) {
        LocalDateTime baseDate = LocalDateTime.ofInstant(currentInstant, zoneId);
        int baseSecond = baseDate.getSecond();
        int baseMinute = baseDate.getMinute();
        int baseHour = baseDate.getHour();
        int baseDay = baseDate.getDayOfMonth();
        int baseMonth = baseDate.getMonthValue();
        int baseYear = baseDate.getYear();

        int second = baseSecond;
        int minute = baseMinute;
        int hour = baseHour;
        int day = baseDay;
        int month = baseMonth;
        int year = baseYear;

        if (this.hasSecondsField) {
            second++;
            second = this.seconds.nextSetBit(second);
            if (second < 0) {
                second = this.seconds.nextSetBit(0);
                minute++;
            }
        } else {
            minute++;
        }

        minute = this.minutes.nextSetBit(minute);
        if (minute < 0) {
            hour++;
            second = this.seconds.nextSetBit(0);
            minute = this.minutes.nextSetBit(0);
        } else if (minute > baseMinute) {
            second = this.seconds.nextSetBit(0);
        }

        hour = this.hours.nextSetBit(hour);
        if (hour < 0) {
            day++;
            second = this.seconds.nextSetBit(0);
            minute = this.minutes.nextSetBit(0);
            hour = this.hours.nextSetBit(0);
        } else if (hour > baseHour) {
            second = this.seconds.nextSetBit(0);
            minute = this.minutes.nextSetBit(0);
        }

        int candidateDay;
        int candidateMonth;
        while (true) {
            candidateMonth = this.months.nextSetBit(month);
            if (candidateMonth < 0) {
                year++;
                second = this.seconds.nextSetBit(0);
                minute = this.minutes.nextSetBit(0);
                hour = this.hours.nextSetBit(0);
                day = 1;
                candidateMonth = this.months.nextSetBit(0);
            } else if (candidateMonth > month) {
                second = this.seconds.nextSetBit(0);
                minute = this.minutes.nextSetBit(0);
                hour = this.hours.nextSetBit(0);
                day = 1;
            }
            month = candidateMonth;
            BitSet adjustedDaysSet = getUpdatedDays(year, month);
            candidateDay = adjustedDaysSet.nextSetBit(day - 1) + 1;
            if (candidateDay < 1) {
                month++;
                second = this.seconds.nextSetBit(0);
                minute = this.minutes.nextSetBit(0);
                hour = this.hours.nextSetBit(0);
                day = 1;
                continue;
            } else if (candidateDay > day) {
                second = this.seconds.nextSetBit(0);
                minute = this.minutes.nextSetBit(0);
                hour = this.hours.nextSetBit(0);
            }
            day = candidateDay;
            Instant possibleNextRun = LocalDateTime
                    .of(year, month, day, hour, minute, second)
                    .atZone(zoneId)
                    .toInstant();
            if (possibleNextRun.isAfter(currentInstant)) return possibleNextRun;
            return next(createdAtInstant, possibleNextRun, zoneId);
        }
    }

    /**
     * Compares this object against the specified object. The result is {@code true}
     * if and only if the argument is not {@code null} and is a {@code Schedule}
     * object that whose seconds, minutes, hours, days, months, and days of
     * weeks sets are equal to those of this schedule.
     * <p>
     * The expression string used to create the schedule is not considered, as two
     * different expressions may produce same schedules.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are the same; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CronExpression))
            return false;
        if (this == obj)
            return true;

        CronExpression cronExpression = (CronExpression) obj;
        return this.seconds.equals(cronExpression.seconds) && this.minutes.equals(cronExpression.minutes)
                && this.hours.equals(cronExpression.hours) && this.days.equals(cronExpression.days)
                && this.months.equals(cronExpression.months) && this.daysOfWeek.equals(cronExpression.daysOfWeek);
    }

    @Override
    public int hashCode() {
        int result = seconds.hashCode();
        result = 31 * result + minutes.hashCode();
        result = 31 * result + hours.hashCode();
        result = 31 * result + days.hashCode();
        result = 31 * result + months.hashCode();
        result = 31 * result + daysOfWeek.hashCode();
        return result;
    }

    private boolean canScheduleActuallyOccur() {
        if (this.daysAndDaysOfWeekRelation == DaysAndDaysOfWeekRelation.UNION || this.days.nextSetBit(0) < 29)
            return true;

        int aYear = LocalDateTime.now(ZoneId.systemDefault()).getYear();
        for (int dayIndex = 29; dayIndex < 31; dayIndex++) {
            if (!this.days.get(dayIndex))
                continue;

            for (int monthIndex = 0; monthIndex <= 12; monthIndex++) {
                if (!this.months.get(monthIndex))
                    continue;

                if (dayIndex + 1 <= YearMonth.of(aYear, monthIndex).lengthOfMonth())
                    return true;
            }
        }
        return false;
    }

    private static BitSet generateDaysOf5Weeks(BitSet daysOfWeek) {
        int weekLength = 7;
        int setLength = weekLength + 31;
        BitSet bitSet = new BitSet(setLength);
        for (int i = 0; i < setLength; i += weekLength) {
            for (int j = 0; j < weekLength; j++) {
                bitSet.set(j + i, daysOfWeek.get(j));
            }
        }
        return bitSet;
    }

    private BitSet getUpdatedDays(int year, int month) {
        LocalDate date = LocalDate.of(year, month, 1);
        int daysOf5WeeksOffset = date.getDayOfWeek().getValue();
        BitSet updatedDays = new BitSet(31);
        updatedDays.or(this.days);
        BitSet monthDaysOfWeeks = this.daysOf5Weeks.get(daysOf5WeeksOffset, daysOf5WeeksOffset + 31);
        if (this.isSpecificLastDayOfMonth || this.daysAndDaysOfWeekRelation == DaysAndDaysOfWeekRelation.INTERSECT) {
            updatedDays.and(monthDaysOfWeeks);
        } else {
            updatedDays.or(monthDaysOfWeeks);
        }
        int dayCountInMonth;
        if (month == Month.FEBRUARY.getValue() /* Feb */) {
            dayCountInMonth = 28;
            if (Year.isLeap(year)) {
                dayCountInMonth++;
            }
        } else {
            // We cannot use lengthOfMonth method with the month Feb
            // because it returns incorrect number of days for years
            // that are dividable by 400 like the year 2000, a bug??
            dayCountInMonth = YearMonth.of(year, month).lengthOfMonth();
        }
        // remove days beyond month length
        for (int j = dayCountInMonth; j < 31; j++) {
            updatedDays.set(j, false);
        }


        if (isLastDayOfMonth) {
            for (int j = 0; j < dayCountInMonth; j++) { // remove all days except last day of month
                updatedDays.set(j, ((j + 1) == dayCountInMonth));
            }
        } else if (isSpecificLastDayOfMonth) { // remove days before the last 7 days
            for (int j = 0; j < dayCountInMonth - 7; j++) {
                updatedDays.set(j, false);
            }
        }
        return updatedDays;
    }
}