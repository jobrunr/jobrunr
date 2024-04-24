package org.jobrunr.utils.carbonaware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DayAheadEnergyPrices {
    private static final Logger LOGGER = LoggerFactory.getLogger(DayAheadEnergyPrices.class);

    private boolean isErrorResponse;
    private String errorMessage;
    private String areaCode;
    private String state;
    private String unit;
    private String timezone;
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
        this.isErrorResponse = false;
    }

    private DayAheadEnergyPrices(String areaCode, String errorMessage) {
        this.areaCode = areaCode;
        this.unit = null;
        this.timezone = null;
        this.hourlyEnergyPrices = null;
        this.errorMessage = errorMessage;
        this.isErrorResponse = true;
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
    public boolean getIsErrorResponse() {
        return isErrorResponse;
    }

    public void setIsErrorResponse(boolean errorResponse) {
        isErrorResponse = errorResponse;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant leastExpensiveHour(Instant from, Instant to) {
        if (hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty()) {
            LOGGER.warn("No hourly energy prices available");
            return null;
        }

        Instant currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        for (HourlyEnergyPrice price : hourlyEnergyPrices) { // list is already sorted by price, so we can stop at the first price that is between `from` and `to`
            if ((price.getDateTime().isAfter(from) || price.getDateTime().equals(from))
                    && (price.getDateTime().isBefore(to) || price.getDateTime().equals(to))
                    && (price.getDateTime().isAfter(currentHour) || price.getDateTime().equals(currentHour))) {
                        return price.getDateTime();
            }
        }
        LOGGER.warn("No hour found between {} and {}", from, to);
        return null;
    }

    @Deprecated
    public Instant getMaxHour() {
        if (hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty()) {
            throw new IllegalStateException("No hourly energy prices available");
        }

        HourlyEnergyPrice maxHourlyPrice = Collections.max(hourlyEnergyPrices, Comparator.comparing(HourlyEnergyPrice::getDateTime));
        return maxHourlyPrice.getDateTime();
    }

    /**
     * Checks if the data are valid and available for the given period
     *
     * @param when The period to check (from, to)
     * @return Returns false:
     *    1. if there is no data available (not available, could not be fetched, etc.)
     *    2. if the period is not within the available data (either `from`> maxHour or `to` < minHour)
     *    3. if the current time is after the last available hour (data are outdated)
     *  Otherwise, returns true
     */
    public boolean hasDataForPeriod(CarbonAwarePeriod when) {
        if (hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty() || isErrorResponse) {
            LOGGER.warn("No hourly energy prices available");
            return false;
        }
        Instant currentHour = Instant.now().truncatedTo(ChronoUnit.HOURS);
        return hourlyEnergyPrices.stream().anyMatch(price -> (price.getDateTime().isAfter(when.getFrom()) || price.getDateTime().equals(when.getFrom()))
                        && (price.getDateTime().isBefore(when.getTo()) || price.getDateTime().equals(when.getTo()))
                        && (price.getDateTime().isAfter(currentHour) || price.getDateTime().equals(currentHour)));

    }


    public static class HourlyEnergyPrice {
        private Instant dateTime;
        private double price;
        private int rank;

        public HourlyEnergyPrice() {}
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
