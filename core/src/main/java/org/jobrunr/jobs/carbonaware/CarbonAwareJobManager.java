package org.jobrunr.jobs.carbonaware;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private static final int DEFAULT_REFRESH_TIME = 18;

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int randomRefreshTimeOffset = ThreadLocalRandom.current().nextInt(360, 721) * 5;
    private volatile CarbonIntensityForecast carbonIntensityForecast;

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

    public ZonedDateTime getDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME)
                .plusSeconds(randomRefreshTimeOffset); // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in a 1-hour period, in 5 sec buckets.
    }

    public ZoneId getTimeZone() {
        return carbonIntensityForecast.getTimezone() == null ? ZoneId.systemDefault() : ZoneId.of(carbonIntensityForecast.getTimezone());
    }

    public void moveToNextState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) return;

        CarbonAwareAwaitingState carbonAwareAwaitingState = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());

        if (handleImmediateSchedulingCases(job, carbonAwareAwaitingState)) return;

        if (!carbonIntensityForecast.hasForecastForPeriod(carbonAwareAwaitingState.getPeriod())) {
            handleUnavailableDataForPeriod(job, carbonAwareAwaitingState);
            return;
        }
        scheduleJobAtOptimalTime(job, carbonAwareAwaitingState);
    }

    private boolean handleImmediateSchedulingCases(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (isDeadlinePassed(carbonAwareAwaitingState)) {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, "Job has passed its deadline, scheduling job now");
            return true;
        }

        if (isDeadlineNextHour(carbonAwareAwaitingState)) {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, "Job is about to pass its deadline, scheduling job now");
            return true;
        }

        return false;
    }

    private void handleUnavailableDataForPeriod(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (shouldWaitWhenDataAreUnavailable(carbonAwareAwaitingState)) {
            LOGGER.trace("No carbon intensity forecast available for region {} at period {} - {}. Waiting for forecast to become available.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
            return;
        }

        if (carbonIntensityForecast.hasError()) {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, format("Could not retrieve carbon intensity forecast: %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonIntensityForecast.getApiResponseStatus().getMessage()));
        } else {
            scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, format("No carbon intensity forecast available for region %s at period %s - %s. Job will be scheduled at pre-defined preferred instant or immediately.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo()));
        }
    }

    private boolean shouldWaitWhenDataAreUnavailable(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        ZonedDateTime currentTimeInZone = ZonedDateTime.now(getTimeZone());
        LocalDate today = currentTimeInZone.toLocalDate();
        LocalDate deadlineDay = carbonAwareAwaitingState.getTo().atZone(getTimeZone()).toLocalDate();
        boolean isDeadlineDay = today.equals(deadlineDay);
        boolean isDayBeforeDeadlineDay = today.equals(deadlineDay.minusDays(1));
        boolean isAfterRefreshTime = currentTimeInZone.getHour() >= DEFAULT_REFRESH_TIME;
        boolean shouldScheduleNow = isDeadlineDay || (isDayBeforeDeadlineDay && isAfterRefreshTime);
        return !shouldScheduleNow;
    }

    private void scheduleJobAtOptimalTime(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        Instant leastExpensiveHour = carbonIntensityForecast.lowestCarbonIntensityInstant(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
        if (leastExpensiveHour != null) {
            carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
        }
        LOGGER.trace("No hour found between {} and {} and greater or equal to current hour. Keep waiting.", carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private void scheduleJobAtPreferredInstant(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState, String reason) {
        Instant scheduleAt = Objects.isNull(carbonAwareAwaitingState.getPreferredInstant()) ? carbonAwareAwaitingState.getFrom() : carbonAwareAwaitingState.getPreferredInstant();
        carbonAwareAwaitingState.moveToNextState(job, scheduleAt, reason);
    }

    private CarbonIntensityApiClient createCarbonIntensityApiClient(CarbonAwareConfigurationReader carbonAwareConfiguration, JsonMapper jsonMapper) {
        return new CarbonIntensityApiClient(carbonAwareConfiguration, jsonMapper);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return Instant.now().isAfter(carbonAwareAwaitingState.getTo());
    }

    private boolean isDeadlineNextHour(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return Instant.now().isAfter(carbonAwareAwaitingState.getTo().minus(1, HOURS));
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
            scheduleCarbonIntensityForecastUpdate(getDailyRefreshTime().toInstant());
        }
    }

    private void updateCarbonIntensityForecastAndScheduleNextUpdate() {
        try {
            updateCarbonIntensityForecast();
        } finally {
            scheduleCarbonIntensityForecastUpdate(getDailyRefreshTime().plusDays(1).toInstant());
        }
    }

    private void scheduleInitialCarbonIntensityRetrieval() {
        scheduledExecutorService.schedule(this::loadCarbonIntensityForecastAndScheduleNextUpdate, 1000, MILLISECONDS);
    }

    private void scheduleCarbonIntensityForecastUpdate(Instant scheduleAt) {
        scheduledExecutorService.schedule(this::updateCarbonIntensityForecastAndScheduleNextUpdate, scheduleAt.toEpochMilli() - Instant.now().toEpochMilli(), MILLISECONDS);
    }
}
