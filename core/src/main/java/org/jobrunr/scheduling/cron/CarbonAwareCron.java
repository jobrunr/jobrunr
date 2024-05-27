package org.jobrunr.scheduling.cron;

import java.time.DayOfWeek;
import java.time.Duration;

import static org.jobrunr.scheduling.cron.CarbonAwareCronUtils.getDuration;

// TODO: WIP remove some methods, there are too many
public class CarbonAwareCron {
    private CarbonAwareCron() {
    }

    //--------------------------------------------------------------------------------//
    //                              DAILY                                             //
    //--------------------------------------------------------------------------------//
    public static CarbonAwareCronExpression daily(int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * * PT%dH PT%dH", allowedHoursToRunEarlier, allowedHoursToRunLater));
    }

    public static CarbonAwareCronExpression daily(int hour, int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * * PT%dH PT%dH", hour, allowedHoursToRunEarlier, allowedHoursToRunLater));
    }

    public static CarbonAwareCronExpression daily(int hour, int minute, int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * * PT%dH PT%dH", minute, hour, allowedHoursToRunEarlier, allowedHoursToRunLater));
    }

    public static CarbonAwareCronExpression dailyBefore(int hour) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * * PT0H PT%dH", hour));
    }

    public static CarbonAwareCronExpression dailyAfter(int hour) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * * PT0S PT%dH", hour, 24 - hour));
    }

    public static CarbonAwareCronExpression dailyBetween(int startHour, int endHour) {
        int duration = endHour - startHour;
        return CarbonAwareCronExpression.create(String.format("0 %d * * * PT0H PT%dH", startHour, duration));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunEarlier(int hoursEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * * PT%dH PT0S", hoursEarlier));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunLater(int hoursLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * * PT0S PT%dH", hoursLater));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunEarlier(int hour, int hoursEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * * PT%dH PT0S", hour, hoursEarlier));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunLater(int hour, int hoursLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * * PT0S PT%dH", hour, hoursLater));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunEarlier(int hour, int minute, int hoursEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * * PT%dH PT0S", minute, hour, hoursEarlier));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunLater(int hour, int minute, int hoursLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * * PT0S PT%dH", minute, hour, hoursLater));
    }

    public static CarbonAwareCronExpression daily(Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 * * * %s %s", allowedToRunEarlier, allowedToRunLater));
    }

    public static CarbonAwareCronExpression daily(int hour, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 %d * * * %s %s", hour, allowedToRunEarlier, allowedToRunLater));
    }

    public static CarbonAwareCronExpression daily(int hour, int minute, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("%d %d * * * %s %s", minute, hour, allowedToRunEarlier, allowedToRunLater));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunEarlier(Duration allowedToRunEarlier) {
        if (allowedToRunEarlier.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 * * * %s PT0S", allowedToRunEarlier));
    }

    public static CarbonAwareCronExpression dailyAllowedToRunLater(Duration allowedToRunLater) {
        if (allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 * * * PT0S %s", allowedToRunLater));
    }

    //--------------------------------------------------------------------------------//
    //                              MONTHLY                                           //
    //--------------------------------------------------------------------------------//
    public static CarbonAwareCronExpression monthly(int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create("0 0 1 * *", Duration.ofDays(allowedDaysToRunEarlier), Duration.ofDays(allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression monthly(int dayOfMonth, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d * *", dayOfMonth), Duration.ofDays(allowedDaysToRunEarlier), Duration.ofDays(allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression monthly(int dayOfMonth, int hour, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d * *", hour, dayOfMonth), Duration.ofDays(allowedDaysToRunEarlier), Duration.ofDays(allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression monthly(int dayOfMonth, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 30 || allowedToRunLater.toDays() > 30) { // Assuming max 30 days
            throw new IllegalArgumentException("Duration cannot be longer than one month for monthly schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 %d * *", dayOfMonth), allowedToRunEarlier, allowedToRunLater);
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunEarlier(int daysEarlier) {
        return CarbonAwareCronExpression.create("0 0 1 * *", Duration.ofDays(daysEarlier), Duration.ofDays(0));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunLater(int daysLater) {
        return CarbonAwareCronExpression.create("0 0 1 * *", Duration.ofDays(0), Duration.ofDays(daysLater));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunEarlier(int dayOfMonth, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d * *", dayOfMonth), Duration.ofDays(daysEarlier), Duration.ofDays(0));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunLater(int dayOfMonth, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d * *", dayOfMonth), Duration.ofDays(0), Duration.ofDays(daysLater));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunEarlier(int dayOfMonth, int hour, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d * *", hour, dayOfMonth), Duration.ofDays(daysEarlier), Duration.ofDays(0));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunLater(int dayOfMonth, int hour, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d * *", hour, dayOfMonth), Duration.ofDays(0), Duration.ofDays(daysLater));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunEarlier(int dayOfMonth, int hour, int minute, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d * *", minute, hour, dayOfMonth), Duration.ofDays(daysEarlier), Duration.ofDays(0));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunLater(int dayOfMonth, int hour, int minute, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d * *", minute, hour, dayOfMonth), Duration.ofDays(0), Duration.ofDays(daysLater));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunHoursEarlier(int dayOfMonth, int hour, int minute, int hoursEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d * * PT%dH PT0H", minute, hour, dayOfMonth, hoursEarlier));
    }

    public static CarbonAwareCronExpression monthlyAllowedToRunHoursLater(int dayOfMonth, int hour, int minute, int hoursLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d * * PT0H PT%dH", minute, hour, dayOfMonth, hoursLater));
    }

    //--------------------------------------------------------------------------------//
    //                              WEEKLY                                            //
    //--------------------------------------------------------------------------------//
    public static CarbonAwareCronExpression weekly(int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * %d P%dDT P%dDT", 1, allowedDaysToRunEarlier, allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression weekly(DayOfWeek dayOfWeek, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * %d P%dDT P%dDT", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression weekly(DayOfWeek dayOfWeek, int hour, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * %d P%dDT P%dDT", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression weekly(DayOfWeek dayOfWeek, int hour, int minute, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * %d P%dDT P%dDT", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater));
    }

    public static CarbonAwareCronExpression weekly(int dayOfWeek, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 6 || allowedToRunLater.toDays() > 6) {
            throw new IllegalArgumentException("Duration cannot be longer than one week for weekly schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 * * %d %s %s", dayOfWeek, allowedToRunEarlier, allowedToRunLater));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunEarlier(int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * 1 P%dDT PT0S", daysEarlier));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunLater(int daysLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * 1 PT0S P%dDT", daysLater));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * %d P%dDT PT0S", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 * * %d PT0S P%dDT", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int hour, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * %d P%dDT PT0S", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int hour, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d * * %d PT0S P%dDT", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int hour, int minute, int daysEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * %d P%dDT PT0S", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier));
    }

    public static CarbonAwareCronExpression weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int hour, int minute, int daysLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d * * %d PT0S P%dDT", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater));
    }

    //--------------------------------------------------------------------------------//
    //                              YEARLY                                            //
    //--------------------------------------------------------------------------------//
    public static CarbonAwareCronExpression yearly(int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return CarbonAwareCronExpression.create("0 0 1 1 *", getDuration(allowedMonthsToRunEarlier, "before"), getDuration(allowedMonthsToRunLater, "after"));
    }

    public static CarbonAwareCronExpression yearly(int month, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 1 %d *", month), getDuration(month, allowedMonthsToRunEarlier, "before"), getDuration(month, allowedMonthsToRunLater, "after"));
    }

    public static CarbonAwareCronExpression yearly(int month, int day, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d %d *", day, month), getDuration(month, allowedMonthsToRunEarlier, "before"), getDuration(month, allowedMonthsToRunLater, "after"));
    }

    public static CarbonAwareCronExpression yearly(int month, int day, int hour, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d %d *", hour, day, month), getDuration(month, day, allowedMonthsToRunEarlier, "before"), getDuration(month, day, allowedMonthsToRunLater, "after"));
    }

    public static CarbonAwareCronExpression yearly(int month, int day, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 365 || allowedToRunLater.toDays() > 365) { // Assuming non-leap year
            throw new IllegalArgumentException("Duration cannot be longer than one year for yearly schedules.");
        }
        return CarbonAwareCronExpression.create(String.format("0 0 %d %d *", day, month), allowedToRunEarlier, allowedToRunLater);
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunEarlier(int monthsEarlier) {
        return CarbonAwareCronExpression.create("0 0 1 1 *", getDuration(monthsEarlier, "before"), getDuration(0, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunLater(int monthsLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 1 1 *", getDuration(0, "before"), getDuration(monthsLater, "after")));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunEarlier(int month, int monthsEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 1 %d *", month), getDuration(month, monthsEarlier, "before"), getDuration(month, 0, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunLater(int month, int monthsLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 1 %d *", month), getDuration(month, 0, "before"), getDuration(month, monthsLater, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunEarlier(int month, int day, int monthsEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d %d *", day, month), getDuration(month, day, monthsEarlier, "before"), getDuration(month, day, 0, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunLater(int month, int day, int monthsLater) {
        return CarbonAwareCronExpression.create(String.format("0 0 %d %d *", day, month), getDuration(month, day, 0, "before"), getDuration(month, day, monthsLater, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunEarlier(int month, int day, int hour, int monthsEarlier) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d %d *", hour, day, month), getDuration(month, day, monthsEarlier, "before"), getDuration(month, day, 0, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunLater(int month, int day, int hour, int monthsLater) {
        return CarbonAwareCronExpression.create(String.format("0 %d %d %d *", hour, day, month), getDuration(month, day, 0, "before"), getDuration(month, day, monthsLater, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunEarlier(int month, int day, int hour, int minute, int monthsEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d %d *", minute, hour, day, month), getDuration(month, day, monthsEarlier, "before"), getDuration(month, day, 0, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunLater(int month, int day, int hour, int minute, int monthsLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d %d *", minute, hour, day, month), getDuration(month, day, 0, "before"), getDuration(month, day, monthsLater, "after"));
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunHoursEarlier(int month, int day, int hour, int minute, int hoursEarlier) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d %d *", minute, hour, day, month), getDuration(month, day, hoursEarlier, "before"), Duration.ZERO);
    }

    public static CarbonAwareCronExpression yearlyAllowedToRunHoursLater(int month, int day, int hour, int minute, int hoursLater) {
        return CarbonAwareCronExpression.create(String.format("%d %d %d %d *", minute, hour, day, month), Duration.ZERO, getDuration(month, day, hoursLater, "after"));
    }
}
