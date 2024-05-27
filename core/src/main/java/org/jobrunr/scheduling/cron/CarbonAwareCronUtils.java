package org.jobrunr.scheduling.cron;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Year;
import java.util.Objects;

public class CarbonAwareCronUtils {

    static Duration getDuration(Integer preferredMonth, int months, String direction) {
        return getDuration(preferredMonth, null, months, direction);
    }

    static Duration getDuration(int months, String direction) {
        return getDuration(null, null, months, direction);
    }

    static Duration getDuration(Integer preferredMonth, Integer preferredDay, int months, String direction) {
        LocalDate today = LocalDate.now();
        LocalDate targetDate;
        int currentMonth = today.getMonthValue();

        int monthToUse = Objects.isNull(preferredMonth) ? currentMonth : preferredMonth;
        int dayToUse = Objects.isNull(preferredDay) ? 1 : preferredDay;

        LocalDate date = LocalDate.of(Year.now().getValue(), monthToUse, dayToUse);

        if (direction.equals("before")) {
            targetDate = date.minusMonths(months);
        } else if (direction.equals("after")) {
            targetDate = date.plusMonths(months);
        } else {
            throw new IllegalArgumentException("Invalid direction. Must be 'before' or 'after'");
        }

        Duration duration = Duration.between(date.atStartOfDay(), targetDate.atStartOfDay());

        if (duration.isNegative()) {
            duration = duration.negated();
        }
        return duration;
    }
}
