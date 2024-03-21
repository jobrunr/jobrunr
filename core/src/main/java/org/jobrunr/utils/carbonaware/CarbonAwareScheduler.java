package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.util.Optional;

public class CarbonAwareScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareScheduler.class);
    private final CarbonAwareApiClient carbonAwareAPIClient;
    private DayAheadEnergyPrices dayAheadEnergyPrices;

    public CarbonAwareScheduler(JsonMapper jsonMapper) {
        this.carbonAwareAPIClient = new CarbonAwareApiClient(jsonMapper);
        Optional<String> area = Optional.ofNullable(CarbonAwareConfiguration.getArea());
        if (CarbonAwareConfiguration.isEnabled()) {
            this.dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(area);
            scheduleDayAheadEnergyPricesFetch(area);
        }
    }

    void scheduleDayAheadEnergyPricesFetch(Optional<String> area) {
        BackgroundJob.scheduleRecurrently("fetch-day-ahead-energy-prices",
                "5 14,16 * * *", //At minute 5, hour 14 and 16
                ZoneId.of("Europe/Brussels"),
                () -> updateDayAheadEnergyPrices(area));
    }

    void updateDayAheadEnergyPrices(Optional<String> area) {
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareAPIClient.fetchLatestDayAheadEnergyPrices(area);
        if (dayAheadEnergyPrices.getIsErrorResponse()){
            return;
        }
        if (dayAheadEnergyPrices.getHourlyEnergyPrices() == null || dayAheadEnergyPrices.getHourlyEnergyPrices().isEmpty()) {
            LOGGER.warn("No hourly energy prices available for area '{}'", area.orElse("unknown"));
            return;
        }
        this.dayAheadEnergyPrices = dayAheadEnergyPrices;
    }

    /**
     * Moves the job to the next state based on the current state and the current day ahead energy prices.
     * TODO: improve javadoc on this method
     * @param job
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

        if (Instant.now().isBefore(carbonAwareAwaitingState.getFrom())) {
            LOGGER.trace("Job {} is not yet ready to be scheduled", job.getId());
            return;
        }

        if (!dayAheadEnergyPrices.hasValidData(carbonAwareAwaitingState.getPeriod())) {
            String msg = String.format("No hourly energy prices available for area '%s' %s. Schedule job now", CarbonAwareConfiguration.getArea(), dayAheadEnergyPrices.getErrorMessage());
            LOGGER.warn(msg);
            carbonAwareAwaitingState.moveToNextState(job, Instant.now(), msg);
            return;
        }

        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getTo());
        if (leastExpensiveHour != null) {
            carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
        }
    }
}

