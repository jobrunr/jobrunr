package org.jobrunr.utils.carbonaware;

import java.time.Instant;
import java.util.List;

import static java.util.Collections.emptyList;

public class DayAheadEnergyPrices {

    private final String area;
    private final Integer hoursAvailable;
    private final String unit;
    private final List<HourlyEnergyPrice> hourlyEnergyPrices;
    private final String errorMessage;

    public DayAheadEnergyPrices() {
        this(null, null, null, emptyList());
    }

    public DayAheadEnergyPrices(String area, Integer hoursAvailable, String unit, List<HourlyEnergyPrice> hourlyEnergyPrices) {
        this.area = area;
        this.hoursAvailable = hoursAvailable;
        this.unit = unit;
        this.hourlyEnergyPrices = hourlyEnergyPrices;
        this.errorMessage = null;
    }

    private DayAheadEnergyPrices(String area, String errorMessage) {
        this.area = area;
        this.hoursAvailable = null;
        this.unit = null;
        this.hourlyEnergyPrices = emptyList();
        this.errorMessage = errorMessage;
    }

    public static DayAheadEnergyPrices error(String message) {
        return null;
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

    public Instant leastExpensiveHour(Instant deadline) {
        return null;
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
