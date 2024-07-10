package org.jobrunr.jobs.carbonaware;

import org.jobrunr.scheduling.carbonaware.CarbonAwarePeriod;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.HOURS;

@SuppressWarnings("FieldMayBeFinal") // because of JSON-B
public class CarbonIntensityForecast {
    private ApiResponseStatus apiResponse;
    private String dataProvider;
    private String dataIdentifier;
    private String displayName;
    private String timezone;
    private Instant nextForecastAvailableAt;
    // use ArrayList instead of List to avoid Jackson deserialization issues (https://github.com/FasterXML/jackson-databind/issues/3892)
    private ArrayList<TimestampedCarbonIntensityForecast> intensityForecast;

    public CarbonIntensityForecast() {
    }

    public CarbonIntensityForecast(ApiResponseStatus apiResponse, String dataProvider, String dataIdentifier, String displayName, String timezone, Instant nextForecastAvailableAt, ArrayList<TimestampedCarbonIntensityForecast> intensityForecast) {
        this.apiResponse = apiResponse;
        this.dataProvider = dataProvider;
        this.dataIdentifier = dataIdentifier;
        this.displayName = displayName;
        this.timezone = timezone;
        this.nextForecastAvailableAt = nextForecastAvailableAt;
        this.intensityForecast = intensityForecast;
    }

    public String getDataProvider() {
        return dataProvider;
    }

    public String getDataIdentifier() {
        return dataIdentifier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTimezone() {
        return timezone;
    }

    public Instant getNextForecastAvailableAt() {
        return nextForecastAvailableAt;
    }

    public List<TimestampedCarbonIntensityForecast> getIntensityForecast() {
        return intensityForecast;
    }

    public boolean hasNoForecast() {
        return intensityForecast == null || intensityForecast.isEmpty();
    }

    public ApiResponseStatus getApiResponseStatus() {
        return apiResponse;
    }

    public boolean hasError() {
        return apiResponse != null && !apiResponse.code.equals("OK");
    }

    public Instant lowestCarbonIntensityInstant(Instant from, Instant to) {
        if (hasNoForecast()) return null;

        // TODO should we take a safe approach and sort the list?
        return intensityForecast.stream()
                .filter(forecast -> isInstantInPeriodAndAfterCurrentHour(forecast.getPeriodStartAt(), from, to))
                .sorted()
                .findFirst()
                .map(TimestampedCarbonIntensityForecast::getPeriodStartAt).orElse(null);
    }

    public boolean hasForecastForPeriod(CarbonAwarePeriod when) {
        if (hasNoForecast()) return false;
        return intensityForecast.stream().anyMatch(
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

    public static class TimestampedCarbonIntensityForecast implements Comparable<TimestampedCarbonIntensityForecast> {
        private Instant periodStartAt;
        private Instant periodEndAt;
        private int rank;

        public TimestampedCarbonIntensityForecast() {
        }

        public TimestampedCarbonIntensityForecast(Instant periodStartAt, Instant periodEndAt, int rank) {
            this.periodStartAt = periodStartAt;
            this.periodEndAt = periodEndAt;
            this.rank = rank;
        }

        public Instant getPeriodStartAt() {
            return periodStartAt;
        }

        public Instant getPeriodEndAt() {
            return periodEndAt;
        }

        public int getRank() {
            return rank;
        }

        @Override
        public int compareTo(TimestampedCarbonIntensityForecast o) {
            return this.rank - o.rank;
        }

        @Override
        public String toString() {
            return "TimestampedCarbonIntensityForecast{" +
                    "periodStartAt=" + periodStartAt +
                    ", periodEndAt=" + periodEndAt +
                    ", rank=" + rank +
                    '}';
        }
    }

    public static class ApiResponseStatus {
        private String code;
        private String message;

        public ApiResponseStatus() {
        }

        public ApiResponseStatus(String code, String message) {
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
