package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Random;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;

@Beta(note = "Scheduling logic for CarbonAware jobs might change in the future. Changes will not affect the API and the end user.")
public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonAwareApiClient carbonAwareAPIClient;
    private DayAheadEnergyPrices dayAheadEnergyPrices;
    private final Random rand = new Random();

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonAwareAPIClient = createCarbonAwareApiClient(jsonMapper);
        Optional<String> areaCode = Optional.ofNullable(this.carbonAwareConfiguration.getAreaCode());
        this.dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(areaCode);
    }

    public Instant getZookeeperTaskDailyRunTime(int baseHour) { // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in 1 hour period, in 5 sec buckets
        int randomOffsetToDistributeLoadOnAPI = rand.nextInt(721) * 5;
        ZonedDateTime baseHourTodayUTC = ZonedDateTime.now(ZoneId.of("Europe/Brussels"))
                .withHour(baseHour).withMinute(0).withSecond(0)
                .plusSeconds(randomOffsetToDistributeLoadOnAPI);
        return baseHourTodayUTC.toInstant();
    }

    public void updateDayAheadEnergyPrices() {
        Optional<String> areaCode = Optional.ofNullable(carbonAwareConfiguration.getAreaCode());
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(areaCode);
        if (!dayAheadEnergyPrices.hasData()) {
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

        if (isDeadlinePassed(state)) {
            scheduleJobImmediately(job, state, "Job has passed its deadline, scheduling job now");
            return;
        }

        if (isDeadlineNextHour(state)) {
            scheduleJobImmediately(job, state, "Job is about to pass its deadline, scheduling job now");
        }

        if (!dayAheadEnergyPrices.hasDataForPeriod(state.getPeriod())) {
            handleUnavailableDataForPeriod(job, state);
            return;
        }
        scheduleJobAtOptimalTime(job, state);
    }

    private void handleUnavailableDataForPeriod(Job job, CarbonAwareAwaitingState state) {
        String msg = String.format("No hourly energy prices available for areaCode '%s' at period %s %s.", carbonAwareConfiguration.getAreaCode(), state, dayAheadEnergyPrices.getErrorMessage());
        if (shouldWaitWhenDataAreUnavailable(state)) {
            LOGGER.warn(msg + " Waiting for prices to become available.");
            return;
        }
        if (isFromTimeAfterNow(state)) {
            scheduleAtFromTime(job, state);
        } else {
            scheduleJobImmediately(job, state, msg + " . Schedule job now.");
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
        boolean shouldSchedule = todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18;
        return !shouldSchedule;
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

    private boolean isDeadlinePassed(CarbonAwareAwaitingState state) {
        return now().isAfter(state.getTo());
    }

    private boolean isDeadlineNextHour(CarbonAwareAwaitingState state) {
        return now().isAfter(state.getTo().minus(1, HOURS));
    }

    private void scheduleJobImmediately(Job job, CarbonAwareAwaitingState state, String reason) {
        LOGGER.warn(reason);
        state.moveToNextState(job, now(), reason);
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

