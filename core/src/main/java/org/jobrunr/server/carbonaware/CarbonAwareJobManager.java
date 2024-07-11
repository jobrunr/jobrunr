package org.jobrunr.server.carbonaware;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Objects.nonNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private static final int DEFAULT_REFRESH_TIME = 19;

    private final int randomRefreshTimeOffset = ThreadLocalRandom.current().nextInt(0, 361) * 5;

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private volatile CarbonIntensityForecast carbonIntensityForecast;
    private volatile Instant nextRefreshTime;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonIntensityApiClient = createCarbonIntensityApiClient(this.carbonAwareConfiguration, jsonMapper);
        this.scheduledExecutorService = newSingleThreadScheduledExecutor(new NamedThreadFactory("carbon-aware-job-manager"));
        this.carbonIntensityForecast = new CarbonIntensityForecast();

        scheduleInitialCarbonIntensityRetrieval();
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    public ZoneId getTimeZone() {
        return nonNull(carbonIntensityForecast.getTimezone()) ? ZoneId.of(carbonIntensityForecast.getTimezone()) : ZoneId.systemDefault();
    }

    public Instant getForecastEndPeriodOrNextRefreshTime() {
        Instant forecastEndPeriod = carbonIntensityForecast.getForecastEndPeriod();
        if (forecastEndPeriod == null || forecastEndPeriod.isBefore(Instant.now())) {
            return nextRefreshTime;
        }
        return forecastEndPeriod;
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
        Instant scheduleAt = Objects.isNull(carbonAwareAwaitingState.getPreferredInstant()) ? carbonAwareAwaitingState.getFrom() : carbonAwareAwaitingState.getPreferredInstant();
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
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchLatestCarbonIntensityForecast();
        if (carbonIntensityForecast.hasNoForecast() && !carbonIntensityForecast.hasError()) {
            LOGGER.warn("No new carbon intensity forecast available. Keeping the old forecast.");
            return;
        }
        this.carbonIntensityForecast = carbonIntensityForecast;
    }

    private void loadCarbonIntensityForecastAndScheduleNextUpdate() {
        try {
            updateCarbonIntensityForecast();
        } finally {
            scheduleCarbonIntensityForecastUpdate(getNextRefreshTimeUsingCarbonIntensityForecastOrFallbackTo(getDefaultDailyRefreshTime()));
        }
    }

    private void updateCarbonIntensityForecastAndScheduleNextUpdate() {
        try {
            updateCarbonIntensityForecast();
        } finally {
            scheduleCarbonIntensityForecastUpdate(getNextRefreshTimeUsingCarbonIntensityForecastOrFallbackTo(getDefaultDailyRefreshTime()).plus(1, DAYS));
        }
    }

    private void scheduleInitialCarbonIntensityRetrieval() {
        Instant scheduleAt = Instant.now().plusSeconds(1);
        scheduledExecutorService.schedule(this::loadCarbonIntensityForecastAndScheduleNextUpdate, getScheduleDelay(scheduleAt), MILLISECONDS);
    }

    private void scheduleCarbonIntensityForecastUpdate(Instant scheduleAt) {
        scheduleCarbonIntensityForecastUpdate(scheduleAt, this::updateCarbonIntensityForecastAndScheduleNextUpdate);
    }

    private void scheduleCarbonIntensityForecastUpdate(Instant scheduleAt, Runnable runnable) {
        LOGGER.trace("Scheduling carbon intensity forecast update for {}.", scheduleAt);
        nextRefreshTime = scheduleAt;
        scheduledExecutorService.schedule(runnable, getScheduleDelay(scheduleAt), MILLISECONDS);
    }

    private long getScheduleDelay(Instant scheduleAt) {
        return scheduleAt.toEpochMilli() - Instant.now().toEpochMilli();
    }

    private Instant getNextRefreshTimeUsingCarbonIntensityForecastOrFallbackTo(Instant defaultNextRefreshTime) {
        Instant nextForecastAvailableAt = carbonIntensityForecast.getNextForecastAvailableAt();
        Instant nextRefreshTime = nonNull(nextForecastAvailableAt) ? nextForecastAvailableAt : defaultNextRefreshTime;
        return nextRefreshTime.plusSeconds(randomRefreshTimeOffset); // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in a 1-hour period, in 5 sec buckets.;
    }

    private Instant getDefaultDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME).toInstant();
    }
}
