package org.jobrunr.scheduling.cron;

import java.time.*;
import java.util.BitSet;
import java.util.Calendar;

import static java.time.Instant.now;

/**
 * Schedule class represents a parsed crontab expression.
 *
 * <p>
 * The schedule class cannot be instantiated using a constructor, a Schedule
 * object can be obtain by using the static {@link #create} method, which parses
 * a crontab expression and creates a Schedule object.
 * <p>
 * Original version <a href="https://github.com/asahaf/javacron">https://github.com/asahaf/javacron</a>
 *
 * @author Ahmed AlSahaf
 * @author Ronald Dehuysser (minor modifications)
 */
public class CronExpression implements Comparable<CronExpression> {

    private enum DaysAndDaysOfWeekRelation {
        INTERSECT, UNION
    }

    private static final CronFieldParser SECONDS_FIELD_PARSER = new CronFieldParser(CronFieldType.SECOND);

    private static final CronFieldParser MINUTES_FIELD_PARSER = new CronFieldParser(CronFieldType.MINUTE);
    private static final CronFieldParser HOURS_FIELD_PARSER = new CronFieldParser(CronFieldType.HOUR);
    private static final CronFieldParser DAYS_FIELD_PARSER = new CronFieldParser(CronFieldType.DAY);
    private static final CronFieldParser MONTHS_FIELD_PARSER = new CronFieldParser(CronFieldType.MONTH);
    private static final CronFieldParser DAY_OF_WEEK_FIELD_PARSER = new CronFieldParser(CronFieldType.DAY_OF_WEEK);

    private CronExpression() {
    }

    private String expression;

    private boolean hasSecondsField;
    private DaysAndDaysOfWeekRelation daysAndDaysOfWeekRelation;
    private BitSet seconds;
    private BitSet minutes;
    private BitSet hours;
    private BitSet days;
    private BitSet months;
    private BitSet daysOfWeek;
    private BitSet daysOf5Weeks;

    /**
     * Parses crontab expression and create a Schedule object representing that
     * expression.
     * <p>
     * The expression string can be 5 fields expression for minutes resolution.
     *
     * <pre>
     *  ┌───────────── minute (0 - 59)
     *  │ ┌───────────── hour (0 - 23)
     *  │ │ ┌───────────── day of the month (1 - 31)
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
     *  │ │ │ ┌───────────── day of the month (1 - 31)
     *  │ │ │ │ ┌───────────── month (1 - 12 or Jan/January - Dec/December)
     *  │ │ │ │ │ ┌───────────── day of the week (0 - 6 or Sun/Sunday - Sat/Saturday)
     *  │ │ │ │ │ │
     *  │ │ │ │ │ │
     *  │ │ │ │ │ │
     * "* * * * * *"
     * </pre>
     *
     * @param expression a crontab expression string used to create Schedule.
     * @return Schedule object created based on the supplied crontab expression.
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
    public static CronExpression create(String expression) {
        if (expression.isEmpty()) {
            throw new InvalidCronExpressionException("empty expression");
        }
        String[] fields = expression.trim().toLowerCase().split("\\s+");
        int count = fields.length;
        if (count > 6 || count < 5) {
            throw new InvalidCronExpressionException(
                    "crontab expression should have 6 fields for (seconds resolution) or 5 fields for (minutes resolution)");
        }
        CronExpression cronExpression = new CronExpression();
        cronExpression.hasSecondsField = count == 6;
        String token;
        int index = 0;
        if (cronExpression.hasSecondsField) {
            token = fields[index++];
            cronExpression.seconds = CronExpression.SECONDS_FIELD_PARSER.parse(token);
        } else {
            cronExpression.seconds = new BitSet(1);
            cronExpression.seconds.set(0);
        }
        token = fields[index++];
        cronExpression.minutes = CronExpression.MINUTES_FIELD_PARSER.parse(token);

        token = fields[index++];
        cronExpression.hours = CronExpression.HOURS_FIELD_PARSER.parse(token);

        token = fields[index++];
        cronExpression.days = CronExpression.DAYS_FIELD_PARSER.parse(token);
        boolean daysStartWithAsterisk = false;
        if (token.startsWith("*"))
            daysStartWithAsterisk = true;

        token = fields[index++];
        cronExpression.months = CronExpression.MONTHS_FIELD_PARSER.parse(token);

        token = fields[index++];
        cronExpression.daysOfWeek = CronExpression.DAY_OF_WEEK_FIELD_PARSER.parse(token);
        boolean daysOfWeekStartAsterisk = false;
        if (token.startsWith("*"))
            daysOfWeekStartAsterisk = true;
        cronExpression.daysOf5Weeks = generateDaysOf5Weeks(cronExpression.daysOfWeek);

        cronExpression.daysAndDaysOfWeekRelation = (daysStartWithAsterisk || daysOfWeekStartAsterisk)
                ? DaysAndDaysOfWeekRelation.INTERSECT
                : DaysAndDaysOfWeekRelation.UNION;

        if (!cronExpression.canScheduleActuallyOccur())
            throw new InvalidCronExpressionException("Cron expression not valid. The specified months do not have the day 30th or the day 31st");
        cronExpression.expression = expression.trim();
        return cronExpression;
    }

    /**
     * Calculates the next occurrence based on the current time (at UTC TimeZone).
     *
     * @return Instant of the next occurrence.
     */
    public Instant next() {
        return next(now(), ZoneOffset.UTC);
    }

    /**
     * Calculates the next occurrence based on the current time (at the given TimeZone).
     *
     * @return Instant of the next occurrence.
     */
    public Instant next(ZoneId zoneId) {
        return next(Instant.now(), zoneId);
    }

    /**
     * Calculates the next occurrence based on provided base time.
     *
     * @param baseInstant Instant object based on which calculating the next occurrence.
     * @return Instant of the next occurrence.
     */
    public Instant next(Instant baseInstant, ZoneId zoneId) {
        LocalDateTime baseDate = LocalDateTime.ofInstant(baseInstant, zoneId);
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
            return LocalDateTime
                    .of(year, month, day, hour, minute, second)
                    .atZone(zoneId)
                    .toInstant();
        }
    }

    /**
     * Compare two {@code Schedule} objects based on next occurrence.
     * <p>
     * The next occurrences are calculated based on the current time.
     *
     * @param anotherCronExpression the {@code Schedule} to be compared.
     * @return the value {@code 0} if this {@code Schedule} next occurrence is equal
     * to the argument {@code Schedule} next occurrence; a value less than
     * {@code 0} if this {@code Schedule} next occurrence is before the
     * argument {@code Schedule} next occurrence; and a value greater than
     * {@code 0} if this {@code Schedule} next occurrence is after the
     * argument {@code Schedule} next occurrence.
     */
    @Override
    public int compareTo(CronExpression anotherCronExpression) {
        if (anotherCronExpression == this) {
            return 0;
        }

        Instant baseInstant = now();
        final Instant nextAnother = anotherCronExpression.next(baseInstant, ZoneOffset.UTC);
        final Instant nextThis = this.next(baseInstant, ZoneOffset.UTC);

        return nextThis.compareTo(nextAnother);
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

    public static boolean isLeapYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        return cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365;
    }

    public int getNumberOfFields() {
        return hasSecondsField ? 6 : 5;
    }

    public String getExpression() {
        return expression;
    }

    private boolean canScheduleActuallyOccur() {
        if (this.daysAndDaysOfWeekRelation == DaysAndDaysOfWeekRelation.UNION || this.days.nextSetBit(0) < 29)
            return true;

        int aYear = LocalDateTime.now().getYear();
        for (int dayIndex = 29; dayIndex < 31; dayIndex++) {
            if (!this.days.get(dayIndex))
                continue;

            for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
                if (!this.months.get(monthIndex))
                    continue;

                if (dayIndex + 1 <= YearMonth.of(aYear, monthIndex + 1).lengthOfMonth())
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
        if (this.daysAndDaysOfWeekRelation == DaysAndDaysOfWeekRelation.INTERSECT) {
            updatedDays.and(monthDaysOfWeeks);
        } else {
            updatedDays.or(monthDaysOfWeeks);
        }
        int i;
        if (month == Month.FEBRUARY.getValue() /* Feb */) {
            i = 28;
            if (isLeapYear(year)) {
                i++;
            }
        } else {
            // We cannot use lengthOfMonth method with the month Feb
            // because it returns incorrect number of days for years
            // that are dividable by 400 like the year 2000, a bug??
            i = YearMonth.of(year, month).lengthOfMonth();
        }
        // remove days beyond month length
        for (; i < 31; i++) {
            updatedDays.set(i, false);
        }
        return updatedDays;
    }
}