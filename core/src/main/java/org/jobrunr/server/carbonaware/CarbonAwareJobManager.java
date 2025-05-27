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
import static java.util.Optional.ofNullable;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;

public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private static final int DEFAULT_REFRESH_TIME = 19;

    private final Duration randomRefreshTimeOffset = Duration.ofMinutes(30).plusSeconds(ThreadLocalRandom.current().nextInt(-300, 300)); // why: 30 minutes plus or min 5 min to make sure they don't hammer the server

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private volatile CarbonIntensityForecast carbonIntensityForecast;
    private volatile Instant nextRefreshTime;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this(new CarbonAwareConfigurationReader(carbonAwareConfiguration), jsonMapper);
    }

    CarbonAwareJobManager(CarbonAwareConfigurationReader carbonAwareConfiguration, JsonMapper jsonMapper) {
        this(carbonAwareConfiguration, new CarbonIntensityApiClient(carbonAwareConfiguration, jsonMapper));
    }

    CarbonAwareJobManager(CarbonAwareConfigurationReader carbonAwareConfiguration, CarbonIntensityApiClient carbonIntensityApiClient) {
        this.carbonAwareConfiguration = carbonAwareConfiguration;
        this.carbonIntensityApiClient = carbonIntensityApiClient;
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

        CarbonAwareAwaitingState state = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());

        if (isDeadlinePassed(state)) {
            scheduleJobAt(job, now(), state, "Passed its deadline, scheduling now.");
        } else if (hasTooSmallScheduleMargin(state)) {
            scheduleJobAt(job, state.getFallbackInstant(), state, "Not enough margin (" + state.getMarginDuration() + ") to be scheduled carbon aware.");
        } else if (carbonIntensityForecast.hasNoForecastForPeriod(state.getFrom(), state.getTo())) {
            scheduleJobAt(job, state.getFallbackInstant(), state, getReasonForMissingForecast(state));
        } else {
            scheduleJobAt(job, idealMoment(state), state, "At the best moment to minimize carbon impact.");
        }
    }

    public boolean hasCarbonIntensityForecastError() {
        return carbonIntensityForecast.hasError();
    }

    private String getReasonForMissingForecast(CarbonAwareAwaitingState state) {
        if (carbonIntensityForecast.hasError()) {
            return format("Could not retrieve carbon intensity forecast: %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonIntensityForecast.getApiResponseStatus().getMessage());
        }
        return format("No carbon intensity forecast available for region %s at period %s - %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonAwareConfiguration.getAreaCode(), state.getFrom(), state.getTo());
    }

    private void scheduleJobAt(Job job, Instant scheduleAt, CarbonAwareAwaitingState state, String reason) {
        state.moveToNextState(job, scheduleAt, reason);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState state) {
        return now().isAfter(state.getTo());
    }

    private boolean hasTooSmallScheduleMargin(CarbonAwareAwaitingState state) {
        if (carbonIntensityForecast.hasNoForecast()) return false;
        Duration margin = state.getMarginDuration();
        return margin.compareTo(carbonIntensityForecast.getForecastInterval().multipliedBy(3)) < 0;
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
        Instant defaultDailyRefreshTime = getDefaultDailyRefreshTime();
        Instant proposedNextRefreshTime = ofNullable(carbonIntensityForecast.getNextForecastAvailableAt()).orElse(defaultDailyRefreshTime);

        if (proposedNextRefreshTime.isBefore(now())) {
            nextRefreshTime = defaultDailyRefreshTime.plus(1, DAYS).plus(randomRefreshTimeOffset);
        } else {
            nextRefreshTime = proposedNextRefreshTime.plus(randomRefreshTimeOffset);
        }
    }

    private Instant idealMoment(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return carbonIntensityForecast.lowestCarbonIntensityInstant(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    @VisibleFor("testing")
    Instant getNextRefreshTime() {
        return nextRefreshTime;
    }

    @VisibleFor("testing")
    ZoneId getTimeZone() {
        return carbonIntensityForecast.getTimezone() != null
                ? ZoneId.of(carbonIntensityForecast.getTimezone())
                : ZoneId.systemDefault();
    }

    private Instant getDefaultDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME).toInstant();
    }
}
