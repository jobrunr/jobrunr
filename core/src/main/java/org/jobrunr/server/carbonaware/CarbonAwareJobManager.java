package org.jobrunr.server.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private static final int DEFAULT_REFRESH_TIME = 19;

    private final int randomRefreshTimeOffset = ThreadLocalRandom.current().nextInt(0, 361) * 5;

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private volatile CarbonIntensityForecast carbonIntensityForecast;
    private volatile Instant nextRefreshTime;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonIntensityApiClient = createCarbonIntensityApiClient(this.carbonAwareConfiguration, jsonMapper);
        nextRefreshTime = Instant.now();
        this.carbonIntensityForecast = new CarbonIntensityForecast();
    }

    public void updateCarbonIntensityForecastIfNecessary() {
        if (!nextRefreshTime.isAfter(Instant.now())) {
            try {
                LOGGER.trace("Updating carbon intensity forecast.");
                updateCarbonIntensityForecast();
            } finally {
                updateNextRefreshTime();
                LOGGER.trace("Carbon intensity forecast updated. Next update is planned for {}", nextRefreshTime);
            }
        }
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    public Instant getTheLaterOfForecastEndAndNextRefreshTime() {
        Instant forecastEndPeriod = carbonIntensityForecast.getForecastEndPeriod();
        return (nonNull(forecastEndPeriod) && forecastEndPeriod.isAfter(nextRefreshTime)) ? forecastEndPeriod : nextRefreshTime;
    }

    public void moveToNextState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("Only jobs in CarbonAwaitingState can move to a next state");
        }

        CarbonAwareAwaitingState carbonAwareAwaitingState = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());

        if (isDeadlinePassed(carbonAwareAwaitingState)) {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, "Job has passed its deadline, scheduling job now");
        } else if (carbonIntensityForecast.hasNoForecastForPeriod(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo())) {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, getReasonForMissingForecast(carbonAwareAwaitingState));
        } else {
            scheduleJobAtOptimalTime(job, carbonAwareAwaitingState);
        }
    }

    private String getReasonForMissingForecast(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (carbonIntensityForecast.hasError()) {
            return format("Could not retrieve carbon intensity forecast: %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonIntensityForecast.getApiResponseStatus().getMessage());
        }
        return format("No carbon intensity forecast available for region %s at period %s - %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private void scheduleJobAtOptimalTime(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        Instant lowestCarbonIntensityInstant = carbonIntensityForecast.lowestCarbonIntensityInstant(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
        carbonAwareAwaitingState.moveToNextState(job, lowestCarbonIntensityInstant);
    }

    private void scheduleJobAtPreferredInstant(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState, String reason) {
        Instant scheduleAt = isNull(carbonAwareAwaitingState.getPreferredInstant()) ? carbonAwareAwaitingState.getFrom() : carbonAwareAwaitingState.getPreferredInstant();
        carbonAwareAwaitingState.moveToNextState(job, scheduleAt, reason);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return Instant.now().isAfter(carbonAwareAwaitingState.getTo());
    }

    private CarbonIntensityApiClient createCarbonIntensityApiClient(CarbonAwareConfigurationReader carbonAwareConfiguration, JsonMapper jsonMapper) {
        return new CarbonIntensityApiClient(carbonAwareConfiguration, jsonMapper);
    }

    @VisibleFor("testing")
    void updateCarbonIntensityForecast() {
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();
        if (carbonIntensityForecast.hasNoForecast() && !carbonIntensityForecast.hasError()) {
            LOGGER.warn("No new carbon intensity forecast available. Keeping the old forecast.");
            return;
        }
        this.carbonIntensityForecast = carbonIntensityForecast;
    }

    private void updateNextRefreshTime() {
        Instant nextForecastAvailableAt = carbonIntensityForecast.getNextForecastAvailableAt();
        Instant defaultDailyRefreshTime = getDefaultDailyRefreshTime();

        Instant proposedNextRefreshTime = nonNull(nextForecastAvailableAt) ? nextForecastAvailableAt : defaultDailyRefreshTime;

        if (proposedNextRefreshTime.isBefore(Instant.now())) {
            nextRefreshTime = defaultDailyRefreshTime.plus(1, DAYS).plusSeconds(randomRefreshTimeOffset);
        } else {
            nextRefreshTime = proposedNextRefreshTime.plusSeconds(randomRefreshTimeOffset);
        }
    }

    @VisibleFor("testing")
    Instant getNextRefreshTime() {
        return nextRefreshTime;
    }

    @VisibleFor("testing")
    ZoneId getTimeZone() {
        return nonNull(carbonIntensityForecast.getTimezone()) ? ZoneId.of(carbonIntensityForecast.getTimezone()) : ZoneId.systemDefault();
    }

    @VisibleFor("testing")
    Instant getDefaultDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME).toInstant();
    }
}
