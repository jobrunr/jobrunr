package org.jobrunr.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

// TODO Use system timezone as default timezone; use timezone from day ahead prices if available
public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private static final int DEFAULT_REFRESH_TIME = 19;

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private final ScheduledExecutorService scheduledExecutorService;
    private final int randomRefreshTimeOffset = new Random().nextInt(721) * 5;
    private volatile DayAheadEnergyPrices dayAheadEnergyPrices;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonIntensityApiClient = createCarbonIntensityApiClient(this.carbonAwareConfiguration, jsonMapper);
        this.scheduledExecutorService = new PlatformThreadPoolJobRunrExecutor(1, 1, "carbon-aware-job-manager");
        this.dayAheadEnergyPrices = new DayAheadEnergyPrices();
        scheduleDayAheadEnergyPricesUpdate(Instant.now().plusSeconds(1));
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    public Instant getDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME)
                .plusSeconds(randomRefreshTimeOffset) // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in a 1-hour period, in 5 sec buckets
                .toInstant();
    }

    public ZoneId getTimeZone() {
        return ZoneId.systemDefault();
    }

    public void moveToNextState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) return;

        CarbonAwareAwaitingState carbonAwareAwaitingState = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());

        if (handleImmediateSchedulingCases(job, carbonAwareAwaitingState)) return;

        if (!dayAheadEnergyPrices.hasDataForPeriod(carbonAwareAwaitingState.getPeriod())) {
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
            LOGGER.trace("No hourly energy prices available for areaCode {} at period {} - {}. Waiting for prices to become available.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
            return;
        }

        scheduleJobAtPreferredInstant(job, carbonAwareAwaitingState, format("No hourly energy prices available for areaCode %s at period %s - %s. Job is scheduled following the .", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo()));
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
        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
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

    private void updateDayAheadEnergyPrices() {
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonIntensityApiClient.fetchLatestDayAheadEnergyPrices();
        scheduleDayAheadEnergyPricesUpdate(getDailyRefreshTime());
        if (dayAheadEnergyPrices.hasNoData()) {
            LOGGER.warn("No new day ahead energy prices available. Keeping the old data.");
            return;
        }
        this.dayAheadEnergyPrices = dayAheadEnergyPrices;
    }

    private void scheduleDayAheadEnergyPricesUpdate(Instant scheduleAt) {
        scheduledExecutorService.schedule(this::updateDayAheadEnergyPrices, scheduleAt.toEpochMilli() - Instant.now().toEpochMilli(), MILLISECONDS);
    }
}
