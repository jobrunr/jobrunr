package org.jobrunr.scheduling.cron;

import java.time.DayOfWeek;
import java.time.Month;

@SuppressWarnings("SameReturnValue")
public class Cron {

    private Cron() {
    }

    public static String daily() {
        return "0 0 * * *";
    }

    public static String daily(int hour) {
        return String.format("0 %d * * *", hour);
    }

    public static String daily(int hour, int minute) {
        return String.format("%d %d * * *", minute, hour);
    }

    public static String hourly() {
        return "0 * * * *";
    }

    public static String hourly(int minute) {
        return String.format("%d * * * *", minute);
    }

    public static String minutely() {
        return "* * * * *";
    }

    public static String every15seconds() {
        return "*/15 * * * * *";
    }

    public static String every30seconds() {
        return "*/30 * * * * *";
    }

    public static String every5minutes() {
        return "*/5 * * * *";
    }

    public static String every10minutes() {
        return "*/10 * * * *";
    }

    public static String every15minutes() {
        return "*/15 * * * *";
    }

    public static String everyHalfHour() {
        return "*/30 * * * *";
    }

    public static String monthly() {
        return "0 0 1 * *";
    }

    public static String monthly(int day) {
        return String.format("0 0 %d * *", day);
    }

    public static String monthly(int day, int hour) {
        return String.format("0 %d %d * *", hour, day);
    }

    public static String monthly(int day, int hour, int minute) {
        return String.format("%d %d %d * *", minute, hour, day);
    }

    public static String lastDayOfTheMonth() {
        return "0 0 L * *";
    }

    public static String lastDayOfTheMonth(int hour) {
        return String.format("0 %d L * *", hour);
    }

    public static String lastDayOfTheMonth(int hour, int minute) {
        return String.format("%d %d L * *", minute, hour);
    }

    public static String weekly() {
        return "0 0 * * 1";
    }

    public static String weekly(DayOfWeek dayOfWeek) {
        return String.format("0 0 * * %d", dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue());
    }

    public static String weekly(DayOfWeek dayOfWeek, int hour) {
        return String.format("0 %d * * %d", hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue());
    }

    public static String weekly(DayOfWeek dayOfWeek, int hour, int minute) {
        return String.format("%d %d * * %d", minute, hour, dayOfWeek.getValue() == 7 ? 0 : dayOfWeek.getValue());
    }

    public static String yearly() {
        return "0 0 1 1 *";
    }

    public static String yearly(Month month) {
        return yearly(month.getValue());
    }

    public static String yearly(Month month, int day) {
        return yearly(month.getValue(), day);
    }

    public static String yearly(Month month, int day, int hour) {
        return yearly(month.getValue(), day, hour);
    }

    public static String yearly(Month month, int day, int hour, int minute) {
        return yearly(month.getValue(), day, hour, minute);
    }

    public static String yearly(int month) {
        return String.format("0 0 1 %d *", month);
    }

    public static String yearly(int month, int day) {
        return String.format("0 0 %d %d *", day, month);
    }

    public static String yearly(int month, int day, int hour) {
        return String.format("0 %d %d %d *", hour, day, month);
    }

    public static String yearly(int month, int day, int hour, int minute) {
        return String.format("%d %d %d %d *", minute, hour, day, month);
    }
}
