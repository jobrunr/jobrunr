package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.utils.annotations.Beta;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * CarbonAwareJobManager contains methods to:
 *  1. Fetch new {@link DayAheadEnergyPrices} from the CarbonAware API
 *  2. Logic to move a job from {@link CarbonAwareAwaitingState} to {@link org.jobrunr.jobs.states.ScheduledState}
 */
@Beta(note = "Scheduling logic for CarbonAware jobs might change in the future. Changes will not affect the API and the end user.")
public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);
    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonAwareApiClient carbonAwareAPIClient;
    private DayAheadEnergyPrices dayAheadEnergyPrices;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonAwareAPIClient = createCarbonAwareApiClient(jsonMapper);
        Optional<String> areaCode = Optional.ofNullable(this.carbonAwareConfiguration.getAreaCode());
        this.dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(areaCode);
    }

    public void updateDayAheadEnergyPrices() {
        Optional<String> areaCode = Optional.ofNullable(carbonAwareConfiguration.getAreaCode());
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(areaCode);
        if (dayAheadEnergyPrices.getIsErrorResponse()){
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
     *  1. If the job has passed its deadline or is about to pass its deadline, schedule the job now
     *  2. If there are no hourly energy prices available for the period (from, to):
     *      - If: (it's the day of the deadline) or (it is the day before the deadline and it's after 18:00), schedule the job now
     *      - Otherwise, wait for prices to become available
     *  3. Schedule the job at the cheapest price available between (from, to)
     * @param job the job to move to the next state
     */
    public void moveToNextState(Job job) {
        JobState jobState = job.getJobState();
        if (!(jobState instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("CarbonAwareScheduler can only handle jobs that are awaiting for the least carbon intense moment");
        }

        CarbonAwareAwaitingState carbonAwareAwaitingState = (CarbonAwareAwaitingState) jobState;

        if (Instant.now().isAfter(carbonAwareAwaitingState.getTo())) {
            LOGGER.warn("Job {} has passed its deadline, schedule job now", job.getId());
            carbonAwareAwaitingState.moveToNextState(job, Instant.now(), "Job has passed its deadline, scheduling job now");
            return;
        }

        if (Instant.now().isAfter(carbonAwareAwaitingState.getTo().minus(1, ChronoUnit.HOURS))) {
            LOGGER.warn("Job {} is about to pass its deadline, schedule job now", job.getId());
            carbonAwareAwaitingState.moveToNextState(job, Instant.now(), "Job is about to pass its deadline, scheduling job now");
            return;
        }

        if (!dayAheadEnergyPrices.hasDataForPeriod(carbonAwareAwaitingState.getPeriod())) {
            String msg = String.format("No hourly energy prices available for areaCode '%s' and period %s %s.", carbonAwareConfiguration.getAreaCode(), carbonAwareAwaitingState, dayAheadEnergyPrices.getErrorMessage());

            ZonedDateTime nowCET = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
            // Check if current day is the previous day of the 'to' instant
            LocalDate today = nowCET.toLocalDate();
            LocalDate deadlineDay = carbonAwareAwaitingState.getTo().atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
            boolean todayIsDeadline = today.equals(deadlineDay);
            boolean todayIsPreviousDayBeforeDeadline = today.equals(deadlineDay.minusDays(1));
            boolean timeIsAfter18 = nowCET.getHour() >= 18;
            boolean todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18 = todayIsPreviousDayBeforeDeadline && timeIsAfter18;

            if (todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18) { //TODO: change this to match zookeeper scheduled task run
                // it's the day before the deadline and it's after 14:00. Schedule job now.
                // If we add more data providers, this logic should be changed, as they might have different schedules.
                msg = msg + " and it's the day before the deadline. Schedule job now.";
                LOGGER.warn(msg);
                carbonAwareAwaitingState.moveToNextState(job, Instant.now(), msg);
                return;
            } else { // wait. Prices might become available later or the next day
                LOGGER.warn(msg + " Waiting for prices to become available.");
                return;
            }
        }

        // from here on, we know that we have valid data
        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
        if (leastExpensiveHour != null) {
            carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
            return;
        }

        LOGGER.info("No hour found between {} and {} and greater or equal to current hour. Keep waiting.", carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    private CarbonAwareApiClient createCarbonAwareApiClient(JsonMapper jsonMapper) {
        return new CarbonAwareApiClient(this.carbonAwareConfiguration.getCarbonAwareApiUrl(), this.carbonAwareConfiguration.getApiClientConnectTimeout(), this.carbonAwareConfiguration.getApiClientReadTimeout(), jsonMapper);
    }
}

