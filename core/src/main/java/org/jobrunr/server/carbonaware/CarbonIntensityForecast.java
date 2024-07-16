package org.jobrunr.server.carbonaware;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static org.jobrunr.utils.CollectionUtils.getLast;
import static org.jobrunr.utils.InstantUtils.isInstantAfterOrEqualToOther;

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

    public CarbonIntensityForecast(ApiResponseStatus apiResponse, String dataProvider, String dataIdentifier, String displayName, String timezone, Instant nextForecastAvailableAt, List<TimestampedCarbonIntensityForecast> intensityForecast) {
        this.apiResponse = apiResponse;
        this.dataProvider = dataProvider;
        this.dataIdentifier = dataIdentifier;
        this.displayName = displayName;
        this.timezone = timezone;
        this.nextForecastAvailableAt = nextForecastAvailableAt;
        this.intensityForecast = isNull(intensityForecast) ? null : new ArrayList<>(intensityForecast);
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

    public Instant getForecastEndPeriod() {
        TimestampedCarbonIntensityForecast last = getLast(intensityForecast);
        if (isNull(last)) return null;
        return last.periodEndAt;
    }

    public ApiResponseStatus getApiResponseStatus() {
        return apiResponse;
    }

    public boolean hasError() {
        return apiResponse != null && !apiResponse.code.equals("OK");
    }

    public Instant lowestCarbonIntensityInstant(Instant startOfPeriod, Instant endOfPeriod) {
        if (hasNoForecast()) return null;

        return getForecastsForPeriod(startOfPeriod, endOfPeriod)
                .sorted()
                .findFirst()
                .map(TimestampedCarbonIntensityForecast::getPeriodStartAt).orElse(null);
    }

    public boolean hasForecastForPeriod(Instant startOfPeriod, Instant endOfPeriod) {
        if (hasNoForecast()) return false;
        return getForecastsForPeriod(startOfPeriod, endOfPeriod).findAny().isPresent();
    }

    public boolean hasNoForecastForPeriod(Instant startOfPeriod, Instant endOfPeriod) {
        return !hasForecastForPeriod(startOfPeriod, endOfPeriod);
    }

    private Stream<TimestampedCarbonIntensityForecast> getForecastsForPeriod(Instant startOfPeriod, Instant endOfPeriod) {
        return intensityForecast.stream().filter(forecast -> isInstantInPeriod(forecast.getPeriodStartAt(), startOfPeriod, endOfPeriod));
    }

    private boolean isInstantInPeriod(Instant instant, Instant startOfPeriod, Instant endOfPeriod) {
        return isInstantAfterOrEqualToOther(instant, startOfPeriod) && instant.isBefore(endOfPeriod);
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
