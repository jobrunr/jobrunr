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

/**
 * CarbonAwareJobManager contains methods to:
 * 1. Fetch new {@link DayAheadEnergyPrices} from the CarbonAware API
 * 2. Logic to move a job from {@link CarbonAwareAwaitingState} to {@link org.jobrunr.jobs.states.ScheduledState}
 */
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
        if (dayAheadEnergyPrices.getIsErrorResponse()) {
            LOGGER.warn("Could not update day ahead energy prices for areaCode '{}': {}",
                    areaCode.orElse("unknown"), dayAheadEnergyPrices.getErrorMessage());
            return;
        }
        if (dayAheadEnergyPrices.getHourlyEnergyPrices() == null || dayAheadEnergyPrices.getHourlyEnergyPrices().isEmpty()) {
            LOGGER.warn("No hourly energy prices available for areaCode '{}'", areaCode.orElse("unknown"));
            return;
        }
        this.dayAheadEnergyPrices = dayAheadEnergyPrices;
    }

    /**
     * Moves the job from {@link CarbonAwareAwaitingState} to {@link org.jobrunr.jobs.states.ScheduledState} based on current {@link DayAheadEnergyPrices}
     * Rules:
     * 1. If the job has passed its deadline or is about to pass its deadline, schedule the job now
     * 2. If there are no hourly energy prices available for the period (from, to):
     * *     - If: (it's the day of the deadline) or (it is the day before the deadline and it's after 18:00), schedule the job now
     * *     - Otherwise, wait for prices to become available
     * 3. Schedule the job at the cheapest price available between (from, to)
     *
     * @param job the job to move to the next state
     */
    public void moveToNextState(Job job) {
        JobState jobState = job.getJobState();
        if (!(jobState instanceof CarbonAwareAwaitingState)) {
            LOGGER.trace("Skipping Job(id={}, jobName='{}') because it is not in state of type CarbonAwareAwaitingState", job.getId(), job.getJobName());
            return;
        }

        CarbonAwareAwaitingState state = (CarbonAwareAwaitingState) jobState;
        LOGGER.trace("Determining best moment to schedule Job(id={}, jobName='{}') so it has the least amount of carbon impact", job.getId(), job.getJobName());

        if (now().isAfter(state.getTo())) {
            LOGGER.warn("Job {} has passed its deadline, schedule job now", job.getId());
            state.moveToNextState(job, now(), "Job has passed its deadline, scheduling job now");
            return;
        }

        if (now().isAfter(state.getTo().minus(1, HOURS))) {
            LOGGER.warn("Job {} is about to pass its deadline, schedule job now", job.getId());
            state.moveToNextState(job, now(), "Job is about to pass its deadline, scheduling job now");
            return;
        }

        if (!dayAheadEnergyPrices.hasDataForPeriod(state.getPeriod())) {
            String msg = String.format("No hourly energy prices available for areaCode '%s' and period %s %s.", carbonAwareConfiguration.getAreaCode(), state, dayAheadEnergyPrices.getErrorMessage());

            ZonedDateTime nowCET = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
            // Check if current day is the previous day of the 'to' instant
            LocalDate today = nowCET.toLocalDate();
            LocalDate deadlineDay = state.getTo().atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
            boolean todayIsDeadline = today.equals(deadlineDay);
            boolean todayIsPreviousDayBeforeDeadline = today.equals(deadlineDay.minusDays(1));
            boolean timeIsAfter18 = nowCET.getHour() >= 18;
            boolean todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18 = todayIsPreviousDayBeforeDeadline && timeIsAfter18;

            if (todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18) {
                // If we add more data providers, this logic should be changed, as they might have different schedules.
                if (state.getFrom().isAfter(now())) {
                    msg = msg + " Schedule job at `from` time.";
                    LOGGER.warn(msg);
                    state.moveToNextState(job, state.getFrom(), msg);
                    return;
                } else {
                    msg = msg + " . Schedule job now.";
                    LOGGER.warn(msg);
                    state.moveToNextState(job, now(), msg);
                    return;
                }
            } else { // wait. Prices might become available later or the next day
                LOGGER.warn(msg + " Waiting for prices to become available.");
                return;
            }
        }

        // from here on, we know that we have valid data
        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(state.getFrom(), state.getTo());
        if (leastExpensiveHour != null) {
            state.moveToNextState(job, leastExpensiveHour);
            return;
        }

        LOGGER.info("No hour found between {} and {} and greater or equal to current hour. Keep waiting.", state.getFrom(), state.getTo());
    }

    public Instant getLeastExpensiveHour(CarbonAwarePeriod period) {
        return dayAheadEnergyPrices.leastExpensiveHour(period);
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    private CarbonAwareApiClient createCarbonAwareApiClient(JsonMapper jsonMapper) {
        return new CarbonAwareApiClient(this.carbonAwareConfiguration.getCarbonAwareApiUrl(), this.carbonAwareConfiguration.getApiClientConnectTimeout(), this.carbonAwareConfiguration.getApiClientReadTimeout(), jsonMapper);
    }
}

