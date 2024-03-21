package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.ChronoUnit;
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
                "10 13,15 * * *", //At minute 10, hour 13 and 15
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

        if (Instant.now().isAfter(carbonAwareAwaitingState.getTo().minus(1, ChronoUnit.HOURS))) {
            LOGGER.warn("Job {} is about to pass its deadline, schedule job now", job.getId());
            carbonAwareAwaitingState.moveToNextState(job, Instant.now(), "Job is about to pass its deadline, scheduling job now");
            return;
        }

        if (!dayAheadEnergyPrices.hasValidData(carbonAwareAwaitingState.getPeriod())) {
            String msg = String.format("No hourly energy prices available for area '%s' %s.",
                    CarbonAwareConfiguration.getArea(), dayAheadEnergyPrices.getErrorMessage());

            ZonedDateTime nowCET = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
            // Check if current day is the previous day of the 'to' instant
            LocalDate today = nowCET.toLocalDate();
            LocalDate deadlineDay = carbonAwareAwaitingState.getTo().atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
            boolean todayIsDeadline = today.equals(deadlineDay);
            boolean todayIsPreviousDayBeforeDeadline = today.equals(deadlineDay.minusDays(1));
            boolean timeIsAfter14 = nowCET.getHour() >= 14;
            boolean todayIsPreviousDayBeforeDeadlineAndTimeIsAfter14 = todayIsPreviousDayBeforeDeadline && timeIsAfter14;

            if (todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter14) {
                // it's the day before the deadline and it's after 14:00. Schedule job now.
                // ENTSO-E publishes day-ahead prices for the next day at 13:00 CET. Allow some delay.
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
        }
    }
}

