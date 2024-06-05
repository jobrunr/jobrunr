package org.jobrunr.utils.carbonaware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

public class DayAheadEnergyPrices {
    private static final Logger LOGGER = LoggerFactory.getLogger(DayAheadEnergyPrices.class);

    private String errorMessage;
    private String areaCode;
    private String state;
    private String unit;
    private String timezone;
    // TODO: add field when data can be refreshed
    // use ArrayList instead of List to avoid Jackson deserialization issues (https://github.com/FasterXML/jackson-databind/issues/3892)
    private ArrayList<HourlyEnergyPrice> hourlyEnergyPrices;

    public DayAheadEnergyPrices() {
        this(null, null, null, null, null);
    }

    public DayAheadEnergyPrices(String areaCode, String state, String timezone, String unit, ArrayList<HourlyEnergyPrice> hourlyEnergyPrices) {
        this.areaCode = areaCode;
        this.state = state;
        this.unit = unit;
        this.timezone = timezone;
        this.hourlyEnergyPrices = hourlyEnergyPrices;
        this.errorMessage = null;
    }

    private DayAheadEnergyPrices(String areaCode, String errorMessage) {
        this.areaCode = areaCode;
        this.unit = null;
        this.timezone = null;
        this.hourlyEnergyPrices = null;
        this.errorMessage = errorMessage;
    }

    public static DayAheadEnergyPrices error(String areaCode, String errorMessage) {
        LOGGER.error("Error fetching day ahead energy prices for areaCode '{}': {}", areaCode, errorMessage);
        return new DayAheadEnergyPrices(areaCode, errorMessage);
    }

    public String getAreaCode() {
        return areaCode;
    }

    public String getUnit() {
        return unit;
    }

    public List<HourlyEnergyPrice> getHourlyEnergyPrices() {
        return hourlyEnergyPrices;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean hasNoData() {
        LOGGER.warn("No hourly energy prices available");
        return hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant leastExpensiveHour(Instant from, Instant to) {
        return leastExpensiveHour(CarbonAwarePeriod.between(from, to));
    }

    public Instant leastExpensiveHour(CarbonAwarePeriod period) {
        if (hasNoData()) return null;

        for (HourlyEnergyPrice price : hourlyEnergyPrices) { // list is already sorted by price, so we can stop at the first price that is between `from` and `to`
            if (isInstantInPeriodAndAfterCurrentHour(price.getDateTime(), period)) {
                return price.getDateTime();
            }
        }
        LOGGER.warn("No hour found between {} and {}", period.getFrom(), period.getTo());
        return null;
    }

    public boolean hasDataForPeriod(CarbonAwarePeriod when) {
        if (hasNoData()) return false;
        return hourlyEnergyPrices.stream().anyMatch(price ->
                isInstantInPeriodAndAfterCurrentHour(price.getDateTime(), when));
    }

    private boolean isInstantInPeriodAndAfterCurrentHour(Instant instant, CarbonAwarePeriod when) {
        return isInstantInRequestedPeriod(instant, when) && isInstantAfterCurrentHour(instant);
    }

    private boolean isInstantInRequestedPeriod(Instant instant, CarbonAwarePeriod when) {
        boolean isAfterStart = instant.isAfter(when.getFrom()) || instant.equals(when.getFrom());
        boolean isBeforeEnd = instant.isBefore(when.getTo()) || instant.equals(when.getTo());
        return isAfterStart && isBeforeEnd;
    }

    private boolean isInstantAfterCurrentHour(Instant instant) {
        Instant currentHour = Instant.now().truncatedTo(HOURS);
        return instant.isAfter(currentHour) || instant.equals(currentHour);
    }


    public static class HourlyEnergyPrice {
        private Instant dateTime;
        private double price;
        private int rank;

        public HourlyEnergyPrice() {
        }

        public HourlyEnergyPrice(Instant dateTime, double price, int rank) {
            this.dateTime = dateTime;
            this.price = price;
            this.rank = rank;
        }

        public Instant getDateTime() {
            return dateTime;
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
                    "dateTime=" + dateTime +
                    ", price=" + price +
                    ", rank=" + rank +
                    '}';
        }
    }
}
