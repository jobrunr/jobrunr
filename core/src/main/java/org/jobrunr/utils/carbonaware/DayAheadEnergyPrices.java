package org.jobrunr.utils.carbonaware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DayAheadEnergyPrices {
    private static final Logger LOGGER = LoggerFactory.getLogger(DayAheadEnergyPrices.class);

    private boolean isErrorResponse;
    private String errorMessage;
    private String area;
    private String state;
    private Integer hoursAvailable;
    private String unit;
    private String timezone;
    // use ArrayList instead of List to avoid Jackson deserialization issues (https://github.com/FasterXML/jackson-databind/issues/3892)
    private ArrayList<HourlyEnergyPrice> hourlyEnergyPrices;

    public DayAheadEnergyPrices() {
        this(null, null, null,null, null, null);
    }

    public DayAheadEnergyPrices(String area, String state, String timezone, Integer hoursAvailable, String unit, ArrayList<HourlyEnergyPrice> hourlyEnergyPrices) {
        this.area = area;
        this.state = state;
        this.hoursAvailable = hoursAvailable;
        this.unit = unit;
        this.timezone = timezone;
        this.hourlyEnergyPrices = hourlyEnergyPrices;
        this.errorMessage = null;
    }

    private DayAheadEnergyPrices(String area, String errorMessage) {
        this.area = area;
        this.hoursAvailable = null;
        this.unit = null;
        this.timezone = null;
        this.hourlyEnergyPrices = null;
        this.errorMessage = errorMessage;
    }

    public static DayAheadEnergyPrices error(String area, String errorMessage) {
        LOGGER.error("Error fetching day ahead energy prices for area '{}': {}", area, errorMessage);
        return new DayAheadEnergyPrices(area, errorMessage);
    }

    public String getArea() {
        return area;
    }

    public Integer getHoursAvailable() {
        return hoursAvailable;
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
        for (HourlyEnergyPrice price : hourlyEnergyPrices) { // list is already sorted by price, so we can stop at the first price that is before `to`
            if ((price.getDateTime().isAfter(from) || price.getDateTime().equals(from))
                    && (price.getDateTime().isBefore(to) || price.getDateTime().equals(to))) {
                return price.getDateTime();
            }
        }
        LOGGER.warn("No hour found before to {}", to);
        return null;
    }

    public Instant getMaxHour() {
        if (hourlyEnergyPrices == null || hourlyEnergyPrices.isEmpty()) {
            throw new IllegalStateException("No hourly energy prices available");
        }

        HourlyEnergyPrice maxHourlyPrice = Collections.max(hourlyEnergyPrices, Comparator.comparing(HourlyEnergyPrice::getDateTime));
        return maxHourlyPrice.getDateTime();
    }

    public boolean hasValidData(CarbonAwarePeriod when) {
        return hourlyEnergyPrices != null
                && !hourlyEnergyPrices.isEmpty()
                && hourlyEnergyPrices.stream().anyMatch(price -> (price.getDateTime().isAfter(when.getFrom()) || price.getDateTime().equals(when.getFrom()))
                        && (price.getDateTime().isBefore(when.getTo()) || price.getDateTime().equals(when.getTo())))
                && !isErrorResponse
                && Instant.now().isBefore(getMaxHour());
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