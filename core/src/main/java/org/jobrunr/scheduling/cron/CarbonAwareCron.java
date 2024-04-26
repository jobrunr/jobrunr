package org.jobrunr.scheduling.cron;

import java.time.DayOfWeek;
import java.time.Duration;

public class CarbonAwareCron {
    private CarbonAwareCron() {
    }

    // TODO: WIP: review and remove some methods: there are too many
    public static String from(String cronExpression, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        return String.format("%s %s %s", cronExpression, allowedToRunEarlier, allowedToRunLater);
    }

    //--------------------------------------------------------------------------------//
    //                              DAILY                                             //
    //--------------------------------------------------------------------------------//
    public static String daily(int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return String.format("0 0 * * * PT%dH PT%dH", allowedHoursToRunEarlier, allowedHoursToRunLater);
    }

    public static String daily(int hour, int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return String.format("0 %d * * * PT%dH PT%dH", hour, allowedHoursToRunEarlier, allowedHoursToRunLater);
    }

    public static String daily(int hour, int minute, int allowedHoursToRunEarlier, int allowedHoursToRunLater) {
        return String.format("%d %d * * * PT%dH PT%dH", minute, hour, allowedHoursToRunEarlier, allowedHoursToRunLater);
    }

    public static String dailyAllowedToRunEarlier(int hoursEarlier) {
        return String.format("0 0 * * * PT%dH PT0S", hoursEarlier);
    }

    public static String dailyAllowedToRunLater(int hoursLater) {
        return String.format("0 0 * * * PT0S PT%dH", hoursLater);
    }

    public static String dailyAllowedToRunEarlier(int hour, int hoursEarlier) {
        return String.format("0 %d * * * PT%dH PT0S", hour, hoursEarlier);
    }

    public static String dailyAllowedToRunLater(int hour, int hoursLater) {
        return String.format("0 %d * * * PT0S PT%dH", hour, hoursLater);
    }

    public static String dailyAllowedToRunEarlier(int hour, int minute, int hoursEarlier) {
        return String.format("%d %d * * * PT%dH PT0S", minute, hour, hoursEarlier);
    }

    public static String dailyAllowedToRunLater(int hour, int minute, int hoursLater) {
        return String.format("%d %d * * * PT0S PT%dH", minute, hour, hoursLater);
    }

    public static String daily(Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return String.format("0 0 * * * %s %s", allowedToRunEarlier, allowedToRunLater);
    }

    public static String daily(int hour, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return String.format("0 %d * * * %s %s", hour, allowedToRunEarlier, allowedToRunLater);
    }

    public static String daily(int hour, int minute, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 0 || allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return String.format("%d %d * * * %s %s", minute, hour, allowedToRunEarlier, allowedToRunLater);
    }

    public static String dailyAllowedToRunEarlier(Duration allowedToRunEarlier) {
        if (allowedToRunEarlier.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return String.format("0 0 * * * %s PT0S", allowedToRunEarlier);
    }

    public static String dailyAllowedToRunLater(Duration allowedToRunLater) {
        if (allowedToRunLater.toDays() > 0) {
            throw new IllegalArgumentException("Duration cannot be longer than one day for daily schedules.");
        }
        return String.format("0 0 * * * PT0S %s", allowedToRunLater);
    }

    //--------------------------------------------------------------------------------//
    //                              MONTHLY                                           //
    //--------------------------------------------------------------------------------//
    public static String monthly(int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 0 1 * * PT%dD PT%dD", allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String monthly(int dayOfMonth, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 0 %d * * PT%dD PT%dD", dayOfMonth, allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String monthly(int dayOfMonth, int hour, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 %d %d * * PT%dD PT%dD", hour, dayOfMonth, allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String monthly(int dayOfMonth, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 30 || allowedToRunLater.toDays() > 30) { // Assuming max 30 days
            throw new IllegalArgumentException("Duration cannot be longer than one month for monthly schedules.");
        }
        return String.format("0 0 %d * * %s %s", dayOfMonth, allowedToRunEarlier, allowedToRunLater);
    }

    public static String monthlyAllowedToRunEarlier(int daysEarlier) {
        return String.format("0 0 1 * * PT%dD PT0S", daysEarlier);
    }

    public static String monthlyAllowedToRunLater(int daysLater) {
        return String.format("0 0 1 * * PT0S PT%dD", daysLater);
    }

    public static String monthlyAllowedToRunEarlier(int dayOfMonth, int daysEarlier) {
        return String.format("0 0 %d * * PT%dD PT0S", dayOfMonth, daysEarlier);
    }

    public static String monthlyAllowedToRunLater(int dayOfMonth, int daysLater) {
        return String.format("0 0 %d * * PT0S PT%dD", dayOfMonth, daysLater);
    }

    public static String monthlyAllowedToRunEarlier(int dayOfMonth, int hour, int daysEarlier) {
        return String.format("0 %d %d * * PT%dD PT0S", hour, dayOfMonth, daysEarlier);
    }

    public static String monthlyAllowedToRunLater(int dayOfMonth, int hour, int daysLater) {
        return String.format("0 %d %d * * PT0S PT%dD", hour, dayOfMonth, daysLater);
    }

    public static String monthlyAllowedToRunEarlier(int dayOfMonth, int hour, int minute, int daysEarlier) {
        return String.format("%d %d %d * * PT%dD PT0S", minute, hour, dayOfMonth, daysEarlier);
    }

    public static String monthlyAllowedToRunLater(int dayOfMonth, int hour, int minute, int daysLater) {
        return String.format("%d %d %d * * PT0S PT%dD", minute, hour, dayOfMonth, daysLater);
    }

    public static String monthlyAllowedToRunHoursEarlier(int dayOfMonth, int hour, int minute, int hoursEarlier) {
        return String.format("%d %d %d * * PT%dH PT0H", minute, hour, dayOfMonth, hoursEarlier);
    }

    public static String monthlyAllowedToRunHoursLater(int dayOfMonth, int hour, int minute, int hoursLater) {
        return String.format("%d %d %d * * PT0H PT%dH", minute, hour, dayOfMonth, hoursLater);
    }

    //--------------------------------------------------------------------------------//
    //                              WEEKLY                                            //
    //--------------------------------------------------------------------------------//
    public static String weekly(int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 0 * * %d PT%dD PT%dD", 1, allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String weekly(DayOfWeek dayOfWeek, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 0 * * %d PT%dD PT%dD", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String weekly(DayOfWeek dayOfWeek, int hour, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("0 %d * * %d PT%dD PT%dD", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String weekly(DayOfWeek dayOfWeek, int hour, int minute, int allowedDaysToRunEarlier, int allowedDaysToRunLater) {
        return String.format("%d %d * * %d PT%dD PT%dD", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), allowedDaysToRunEarlier, allowedDaysToRunLater);
    }

    public static String weekly(int dayOfWeek, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 6 || allowedToRunLater.toDays() > 6) {
            throw new IllegalArgumentException("Duration cannot be longer than one week for weekly schedules.");
        }
        return String.format("0 0 * * %d %s %s", dayOfWeek, allowedToRunEarlier, allowedToRunLater);
    }

    public static String weeklyAllowedToRunEarlier(int daysEarlier) {
        return String.format("0 0 * * 1 PT%dD PT0S", daysEarlier);
    }

    public static String weeklyAllowedToRunLater(int daysLater) {
        return String.format("0 0 * * 1 PT0S PT%dD", daysLater);
    }

    public static String weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int daysEarlier) {
        return String.format("0 0 * * %d PT%dD PT0S", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier);
    }

    public static String weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int daysLater) {
        return String.format("0 0 * * %d PT0S PT%dD", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater);
    }

    public static String weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int hour, int daysEarlier) {
        return String.format("0 %d * * %d PT%dD PT0S", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier);
    }

    public static String weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int hour, int daysLater) {
        return String.format("0 %d * * %d PT0S PT%dD", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater);
    }

    public static String weeklyAllowedToRunEarlier(DayOfWeek dayOfWeek, int hour, int minute, int daysEarlier) {
        return String.format("%d %d * * %d PT%dD PT0S", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysEarlier);
    }

    public static String weeklyAllowedToRunLater(DayOfWeek dayOfWeek, int hour, int minute, int daysLater) {
        return String.format("%d %d * * %d PT0S PT%dD", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue(), daysLater);
    }

    //--------------------------------------------------------------------------------//
    //                              YEARLY                                            //
    //--------------------------------------------------------------------------------//
    public static String yearly(int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return String.format("0 0 1 1 * PT%dM PT%dM", allowedMonthsToRunEarlier, allowedMonthsToRunLater);
    }

    public static String yearly(int month, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return String.format("0 0 1 %d * PT%dM PT%dM", month, allowedMonthsToRunEarlier, allowedMonthsToRunLater);
    }

    public static String yearly(int month, int day, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return String.format("0 0 %d %d * PT%dM PT%dM", day, month, allowedMonthsToRunEarlier, allowedMonthsToRunLater);
    }

    public static String yearly(int month, int day, int hour, int allowedMonthsToRunEarlier, int allowedMonthsToRunLater) {
        return String.format("0 %d %d %d * PT%dM PT%dM", hour, day, month, allowedMonthsToRunEarlier, allowedMonthsToRunLater);
    }

    public static String yearly(int month, int day, Duration allowedToRunEarlier, Duration allowedToRunLater) {
        if (allowedToRunEarlier.toDays() > 365 || allowedToRunLater.toDays() > 365) { // Assuming non-leap year
            throw new IllegalArgumentException("Duration cannot be longer than one year for yearly schedules.");
        }
        return String.format("0 0 %d %d * %s %s", day, month, allowedToRunEarlier, allowedToRunLater);
    }

    public static String yearlyAllowedToRunEarlier(int monthsEarlier) {
        return String.format("0 0 1 1 * PT%dM PT0S", monthsEarlier);
    }

    public static String yearlyAllowedToRunLater(int monthsLater) {
        return String.format("0 0 1 1 * PT0S PT%dM", monthsLater);
    }

    public static String yearlyAllowedToRunEarlier(int month, int monthsEarlier) {
        return String.format("0 0 1 %d * PT%dM PT0S", month, monthsEarlier);
    }

    public static String yearlyAllowedToRunLater(int month, int monthsLater) {
        return String.format("0 0 1 %d * PT0S PT%dM", month, monthsLater);
    }

    public static String yearlyAllowedToRunEarlier(int month, int day, int monthsEarlier) {
        return String.format("0 0 %d %d * PT%dM PT0S", day, month, monthsEarlier);
    }

    public static String yearlyAllowedToRunLater(int month, int day, int monthsLater) {
        return String.format("0 0 %d %d * PT0S PT%dM", day, month, monthsLater);
    }

    public static String yearlyAllowedToRunEarlier(int month, int day, int hour, int monthsEarlier) {
        return String.format("0 %d %d %d * PT%dM PT0S", hour, day, month, monthsEarlier);
    }

    public static String yearlyAllowedToRunLater(int month, int day, int hour, int monthsLater) {
        return String.format("0 %d %d %d * PT0S PT%dM", hour, day, month, monthsLater);
    }

    public static String yearlyAllowedToRunEarlier(int month, int day, int hour, int minute, int monthsEarlier) {
        return String.format("%d %d %d %d * PT%dM PT0S", minute, hour, day, month, monthsEarlier);
    }

    public static String yearlyAllowedToRunLater(int month, int day, int hour, int minute, int monthsLater) {
        return String.format("%d %d %d %d * PT0S PT%dM", minute, hour, day, month, monthsLater);
    }

    public static String yearlyAllowedToRunHoursEarlier(int month, int day, int hour, int minute, int hoursEarlier) {
        return String.format("%d %d %d %d * PT%dH PT0H", minute, hour, day, month, hoursEarlier);
    }

    public static String yearlyAllowedToRunHoursLater(int month, int day, int hour, int minute, int hoursLater) {
        return String.format("%d %d %d %d * PT0H PT%dH", minute, hour, day, month, hoursLater);
    }
}
