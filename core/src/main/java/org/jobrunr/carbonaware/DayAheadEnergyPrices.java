package org.jobrunr.carbonaware;

import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

public class DayAheadEnergyPrices {

    private String areaCode;
    private String state;
    private String unit;
    private String timezone;
    // TODO: add field when data can be refreshed
    // use ArrayList instead of List to avoid Jackson deserialization issues (https://github.com/FasterXML/jackson-databind/issues/3892)
    private ArrayList<HourlyEnergyPrice> hourlyEnergyPrices;

    // TODO constructor should not be public
    public DayAheadEnergyPrices() {
        this(null, null, null, null, null);
    }

    public DayAheadEnergyPrices(String areaCode, String state, String timezone, String unit, ArrayList<HourlyEnergyPrice> hourlyEnergyPrices) {
        this.areaCode = areaCode;
        this.state = state;
        this.unit = unit;
        this.timezone = timezone;
        this.hourlyEnergyPrices = hourlyEnergyPrices;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public String getUnit() {
        return unit;
    }

    public String getTimezone() {
        return timezone;
    }

    public List<HourlyEnergyPrice> getHourlyEnergyPrices() {
        return hourlyEnergyPrices;
    }

    public boolean hasNoData() {
        return hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty();
    }

    public Instant leastExpensiveHour(CarbonAwarePeriod period) {
        return leastExpensiveHour(period.getFrom(), period.getTo());
    }

    public Instant leastExpensiveHour(Instant from, Instant to) {
        if (hasNoData()) return null;

        // TODO should we take a safe approach and sort the list?
        return hourlyEnergyPrices.stream()
                .filter(price -> isInstantInPeriodAndAfterCurrentHour(price.getPeriodStartAt(), from, to))
                .findFirst() // list is already sorted by price, so we can stop at the first price that is between `from` and `to`
                .map(HourlyEnergyPrice::getPeriodStartAt).orElse(null);
    }

    public boolean hasDataForPeriod(CarbonAwarePeriod when) {
        if (hasNoData()) return false;
        return hourlyEnergyPrices.stream().anyMatch(
                price -> isInstantInPeriodAndAfterCurrentHour(price.getPeriodStartAt(), when.getFrom(), when.getTo())
        );
    }

    private boolean isInstantInPeriodAndAfterCurrentHour(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        return isInstantInRequestedPeriod(instant, startOfPeriod, endOfPeriod) && isInstantAfterCurrentHour(instant);
    }

    private boolean isInstantInRequestedPeriod(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        boolean isAfterStart = !instant.isBefore(startOfPeriod);
        boolean isBeforeEnd = !instant.isAfter(endOfPeriod);
        return isAfterStart && isBeforeEnd;
    }

    private boolean isInstantAfterCurrentHour(Instant instant) {
        Instant currentHour = Instant.now().truncatedTo(HOURS);
        return !instant.isBefore(currentHour);
    }


    public static class HourlyEnergyPrice {
        private Instant periodStartAt;
        private double price;
        private int rank;

        public HourlyEnergyPrice() {
        }

        public HourlyEnergyPrice(Instant periodStartAt, double price, int rank) {
            this.periodStartAt = periodStartAt;
            this.price = price;
            this.rank = rank;
        }

        public Instant getPeriodStartAt() {
            return periodStartAt;
        }

        public double getPrice() {
            return price;
        }

        public int getRank() {
            return rank;
        }

        public int compareTo(HourlyEnergyPrice other) {
            return Double.compare(this.price, other.price);
        }

        @Override
        public String toString() {
            return "HourlyEnergyPrice{" +
                    "periodStartAt=" + periodStartAt +
                    ", price=" + price +
                    ", rank=" + rank +
                    '}';
        }
    }
}
