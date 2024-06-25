package org.jobrunr.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;

public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonAwareApiClient carbonAwareApiClient;
    private DayAheadEnergyPrices dayAheadEnergyPrices;
    private final Random rand = new Random();

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonAwareApiClient = createCarbonAwareApiClient(jsonMapper);
        updateDayAheadEnergyPrices();
    }

    public Instant getZookeeperTaskDailyRunTime(int baseHour) { // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in 1 hour period, in 5 sec buckets
        int randomOffsetToDistributeLoadOnApi = rand.nextInt(721) * 5;
        ZonedDateTime baseHourTodayUTC = ZonedDateTime.now(ZoneId.of("Europe/Brussels"))
                .withHour(baseHour).withMinute(0).withSecond(0)
                .plusSeconds(randomOffsetToDistributeLoadOnApi);
        return baseHourTodayUTC.toInstant();
    }

    public void updateDayAheadEnergyPrices() {
        Optional<String> areaCode = Optional.ofNullable(carbonAwareConfiguration.getAreaCode());
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices(areaCode);
        if (Objects.isNull(dayAheadEnergyPrices) || dayAheadEnergyPrices.hasNoData()) {
            LOGGER.warn("No new day ahead energy prices available. Keeping the old data.");
            return;
        }
        this.dayAheadEnergyPrices = dayAheadEnergyPrices;
    }

    public void moveToNextState(Job job) {
        JobState jobState = job.getJobState();
        if (!isJobInAwaitingState(job)) {
            return;
        }

        CarbonAwareAwaitingState state = (CarbonAwareAwaitingState) jobState;
        LOGGER.trace("Determining best moment to schedule Job(id={}, jobName='{}') so it has the least amount of carbon impact", job.getId(), job.getJobName());
        if (handleImmediateSchedulingCases(job, state))
            return;

        if (!dayAheadEnergyPrices.hasDataForPeriod(state.getPeriod())) {
            handleUnavailableDataForPeriod(job, state);
            return;
        }
        scheduleJobAtOptimalTime(job, state);
    }

    private boolean handleImmediateSchedulingCases(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (isDeadlinePassed(carbonAwareAwaitingState)) {
            scheduleJobImmediately(job, carbonAwareAwaitingState, "Job has passed its deadline, scheduling job now");
            return true;
        }

        if (isDeadlineNextHour(carbonAwareAwaitingState)) {
            scheduleJobImmediately(job, carbonAwareAwaitingState, "Job is about to pass its deadline, scheduling job now");
            return true;
        }

        return false;
    }

    private void handleUnavailableDataForPeriod(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (shouldWaitWhenDataAreUnavailable(carbonAwareAwaitingState)) {
            LOGGER.warn("No hourly energy prices available for areaCode {} at period {} - {}. Waiting for prices to become available.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
            return;
        }
        if (isFromTimeAfterNow(carbonAwareAwaitingState)) {
            scheduleAtFromTime(job, carbonAwareAwaitingState);
        } else {
            scheduleJobImmediately(job, carbonAwareAwaitingState, format("No hourly energy prices available for areaCode %s at period %s - %s. Job is scheduled at current instant.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo()));
        }
    }

    private boolean shouldWaitWhenDataAreUnavailable(CarbonAwareAwaitingState state) {
        ZonedDateTime nowCET = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
        LocalDate today = nowCET.toLocalDate();
        LocalDate deadlineDay = state.getTo().atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
        boolean todayIsDeadline = today.equals(deadlineDay);
        boolean todayIsPreviousDayBeforeDeadline = today.equals(deadlineDay.minusDays(1));
        boolean timeIsAfter18 = nowCET.getHour() >= 18;
        boolean todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18 = todayIsPreviousDayBeforeDeadline && timeIsAfter18;
        boolean shouldScheduleNow = todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18;
        return !shouldScheduleNow;
    }

    private void scheduleJobAtOptimalTime(Job job, CarbonAwareAwaitingState state) {
        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(state.getFrom(), state.getTo());
        if (leastExpensiveHour != null) {
            state.moveToNextState(job, leastExpensiveHour);
        }
        LOGGER.info("No hour found between {} and {} and greater or equal to current hour. Keep waiting.", state.getFrom(), state.getTo());
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    private CarbonAwareApiClient createCarbonAwareApiClient(JsonMapper jsonMapper) {
        return new CarbonAwareApiClient(this.carbonAwareConfiguration.getCarbonAwareApiUrl(), this.carbonAwareConfiguration.getApiClientConnectTimeout(), this.carbonAwareConfiguration.getApiClientReadTimeout(), jsonMapper);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return now().isAfter(carbonAwareAwaitingState.getTo());
    }

    private boolean isDeadlineNextHour(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return now().isAfter(carbonAwareAwaitingState.getTo().minus(1, HOURS));
    }

    private void scheduleJobImmediately(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState, String reason) {
        Instant scheduleAt = Objects.isNull(carbonAwareAwaitingState.getPreferredInstant()) ? now() : carbonAwareAwaitingState.getPreferredInstant();
        carbonAwareAwaitingState.moveToNextState(job, scheduleAt, reason);
    }

    private void scheduleAtFromTime(Job job, CarbonAwareAwaitingState state) {
        LOGGER.warn("JobId: {}. No hourly energy prices available for areaCode '{}'. Schedule job at `from` time {}.", job.getId(), carbonAwareConfiguration.getAreaCode(), state.getFrom());
        state.moveToNextState(job, state.getFrom(), "JobId: " + job.getId() + ". No hourly energy prices available for areaCode '" + carbonAwareConfiguration.getAreaCode() + "'. Schedule job at `from` time.");
    }

    private boolean isJobInAwaitingState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            LOGGER.trace("Skipping Job(id={}, jobName='{}') because it is not in state of type CarbonAwareAwaitingState", job.getId(), job.getJobName());
            return false;
        }
        return true;
    }

    private boolean isFromTimeAfterNow(CarbonAwareAwaitingState state) {
        return state.getFrom().isAfter(now());
    }
}
