package org.jobrunr.server.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;

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
        this.nextRefreshTime = now();
        this.carbonIntensityForecast = new CarbonIntensityForecast();
    }

    public void updateCarbonIntensityForecastIfNecessary() {
        if (isInstantBeforeOrEqualTo(nextRefreshTime, now())) {
            LOGGER.trace("Updating carbon intensity forecast.");
            updateCarbonIntensityForecast();
            updateNextRefreshTime();
            LOGGER.trace("Carbon intensity forecast updated. Next update is planned for {}", nextRefreshTime);
        }
    }

    public boolean isEnabled() {
        return carbonAwareConfiguration.isEnabled();
    }

    public boolean isDisabled() {
        return !isEnabled();
    }

    public Instant getAvailableForecastEndTime() {
        Instant forecastEndPeriod = carbonIntensityForecast.getForecastEndPeriod();
        return (forecastEndPeriod != null && forecastEndPeriod.isAfter(nextRefreshTime)) ? forecastEndPeriod : nextRefreshTime;
    }

    public void moveToNextState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("Only jobs in CarbonAwaitingState can move to a next state");
        }

        CarbonAwareAwaitingState carbonAwareAwaitingState = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());

        if (isDeadlinePassed(carbonAwareAwaitingState)) {
            scheduleJobAt(job, now(), carbonAwareAwaitingState, "Job has passed its deadline, scheduling job now");
        } else if (hasTooSmallScheduleMargin(carbonAwareAwaitingState)) {
            Duration carbonAwareMarginDuration = getCarbonAwareMarginDuration(carbonAwareAwaitingState);
            scheduleJobAt(job, fallbackInstant(carbonAwareAwaitingState), carbonAwareAwaitingState, "Job does not have enough margin (" + carbonAwareMarginDuration + ") to be scheduled carbon aware, scheduling job at start of CarbonAwarePeriod");
        } else if (carbonIntensityForecast.hasNoForecastForPeriod(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo())) {
            scheduleJobAt(job, fallbackInstant(carbonAwareAwaitingState), carbonAwareAwaitingState, getReasonForMissingForecast(carbonAwareAwaitingState));
        } else {
            Instant idealMoment = idealMoment(carbonAwareAwaitingState);
            scheduleJobAt(job, idealMoment, carbonAwareAwaitingState, "Job scheduled at " + idealMoment + " to minimize carbon impact.");
        }
    }

    public boolean hasCarbonIntensityForecastError() {
        return carbonIntensityForecast.hasError();
    }

    private String getReasonForMissingForecast(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (carbonIntensityForecast.hasError()) {
            return format("Could not retrieve carbon intensity forecast: %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonIntensityForecast.getApiResponseStatus().getMessage());
        }
        return format("No carbon intensity forecast available for region %s at period %s - %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private void scheduleJobAt(Job job, Instant instant, CarbonAwareAwaitingState carbonAwareAwaitingState, String reason) {
        carbonAwareAwaitingState.moveToNextState(job, instant, reason);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return now().isAfter(carbonAwareAwaitingState.getTo());
    }

    private boolean hasTooSmallScheduleMargin(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (carbonIntensityForecast.hasNoForecast()) return false;
        Duration carbonAwareMarginDuration = getCarbonAwareMarginDuration(carbonAwareAwaitingState);
        return carbonAwareMarginDuration.compareTo(carbonIntensityForecast.getForecastInterval().multipliedBy(3)) < 0;
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

        Instant proposedNextRefreshTime = nextForecastAvailableAt != null ? nextForecastAvailableAt : defaultDailyRefreshTime;

        if (proposedNextRefreshTime.isBefore(now())) {
            nextRefreshTime = defaultDailyRefreshTime.plus(1, DAYS).plusSeconds(randomRefreshTimeOffset);
        } else {
            nextRefreshTime = proposedNextRefreshTime.plusSeconds(randomRefreshTimeOffset);
        }
    }

    private static Duration getCarbonAwareMarginDuration(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return Duration.between(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private Instant idealMoment(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return carbonIntensityForecast.lowestCarbonIntensityInstant(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private static Instant fallbackInstant(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if(carbonAwareAwaitingState.getPreferredInstant() != null) return carbonAwareAwaitingState.getPreferredInstant();
        return carbonAwareAwaitingState.getFrom();
    }

    @VisibleFor("testing")
    Instant getNextRefreshTime() {
        return nextRefreshTime;
    }

    @VisibleFor("testing")
    ZoneId getTimeZone() {
        return carbonIntensityForecast.getTimezone() != null ? ZoneId.of(carbonIntensityForecast.getTimezone()) : ZoneId.systemDefault();
    }

    private Instant getDefaultDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME).toInstant();
    }
}
