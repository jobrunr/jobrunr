package org.jobrunr.jobs.carbonaware;

import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class DayAheadEnergyPrices {

    private String areaCode;
    private String state;
    private String timezone;
    // TODO: add field when data can be refreshed
    // use ArrayList instead of List to avoid Jackson deserialization issues (https://github.com/FasterXML/jackson-databind/issues/3892)
    private ArrayList<HourlyEnergyPrice> hourlyEnergyPrices;
    private Error error;

    public DayAheadEnergyPrices() {
        this(null, null, null, null, null);
    }

    public DayAheadEnergyPrices(String areaCode, String state, String timezone, ArrayList<HourlyEnergyPrice> hourlyEnergyPrices, Error error) {
        this.areaCode = areaCode;
        this.state = state;
        this.timezone = timezone;
        this.hourlyEnergyPrices = hourlyEnergyPrices;
        this.error = error;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public String getState() {
        return state;
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

    public Error getError() {
        return error;
    }

    public boolean hasError() {
        return error != null;
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
        private String unit;
        private int rank;

        public HourlyEnergyPrice() {
        }

        public HourlyEnergyPrice(Instant periodStartAt, double price, String unit, int rank) {
            this.periodStartAt = periodStartAt;
            this.price = price;
            this.unit = unit;
            this.rank = rank;
        }

        public Instant getPeriodStartAt() {
            return periodStartAt;
        }

        public double getPrice() {
            return price;
        }

        public String getUnit() {
            return unit;
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

    public static class Error {
        private String code;
        private String message;

        public Error() {
        }

        public Error(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

}
