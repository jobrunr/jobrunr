package org.jobrunr.utils.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.utils.TimeProvider;
import org.jobrunr.utils.annotations.VisibleFor;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

public class CarbonAwareScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareScheduler.class);
    private final CarbonAwareApiClient carbonAwareAPIClient;
    private DayAheadEnergyPrices dayAheadEnergyPrices;
    private final TimeProvider timeProvider;

    public CarbonAwareScheduler(JsonMapper jsonMapper, TimeProvider timeProvider) {
        this.carbonAwareAPIClient = new CarbonAwareApiClient(jsonMapper);
        this.timeProvider = timeProvider;
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

        if (!dayAheadEnergyPrices.getArea().equals(CarbonAwareConfiguration.getArea())) {
            LOGGER.warn("Area of job {} has changed, updating day ahead energy prices", job.getId());
            updateDayAheadEnergyPrices(Optional.of(CarbonAwareConfiguration.getArea()));
        }

        // if max hour we have is before the next hour
        if (dayAheadEnergyPrices.getMaxHour().isBefore(timeProvider.now().plus(Duration.ofHours(1)))) {
            LOGGER.warn("Day ahead energy prices are outdated, updating day ahead energy prices");
            //TODO: update values or schedule now?? I think we should schedule now, because suppose that we cannot update values (they are not available).
            // Every job will try this and fails for no reason.
            //updateDayAheadEnergyPrices(Optional.of(CarbonAwareConfiguration.getArea()));
            job.enqueue();
        }

        if (carbonAwareAwaitingState.getDeadline().isBefore(timeProvider.now())) {
            LOGGER.warn("Job {} has passed its deadline", job.getId());
            //TODO schedule now ??
            job.enqueue();
            return;
        }

        if (dayAheadEnergyPrices.getIsErrorResponse() || dayAheadEnergyPrices.getHourlyEnergyPrices() == null || dayAheadEnergyPrices.getHourlyEnergyPrices().isEmpty()) {
            //TODO: in this case schedule now or at deadline?? -> Probably wait until deadline, because maybe we can get data in the meantime
            LOGGER.warn("No hourly energy prices available for area '{}'. Keep waiting.", CarbonAwareConfiguration.getArea());
            return;
        }

        // If Sunday is within the deadline, but we don't have hours available for Sunday, we should wait until they are available to schedule the job
        // In contrast, if Sunday is within the deadline, and we have hours available for Sunday, we should schedule the job at the least expensive hour
        // ENTSO-E provides either a full day (24 hours) or nothing. So, we check if the max hour is less than the upcomingSunday  at a random hour (15:00).
        // If max hour is greater than upcomingSunday15, that means we have data for Sunday.
        if (waitJobIfDayAvailableAndDataNotAvailable(DayOfWeek.SUNDAY, job, carbonAwareAwaitingState)) return;
        // do the same stuff for Saturday
        if (waitJobIfDayAvailableAndDataNotAvailable(DayOfWeek.SATURDAY, job, carbonAwareAwaitingState)) return;

        // at 1-2pm Belgium time we get 24H, the hours of the next day. So, at maximum we will have more than 24h of data.
        // Suppose we are at a country with UTC time. So, at 12 local time we will have 24h + 12h  = 36h of data.
        if (carbonAwareAwaitingState.getDeadline().isAfter(dayAheadEnergyPrices.getMaxHour())) {
            // TODO: if deadline is far in the future, we should schedule now or wait until we have data for the deadline?
            LOGGER.warn("Job {} has a deadline later than the last available hour of day ahead energy prices, keep waiting", job.getId());
            return;
        }

        // From here on we know that we have data and that the deadline is within the available data. Just schedule
        // TODO: schedule job

        if (dayAheadEnergyPrices.getErrorMessage() != null) {
            carbonAwareAwaitingState.moveToNextState(job, carbonAwareAwaitingState.getDeadline(), "No carbon intensity info available (" + dayAheadEnergyPrices.getErrorMessage() + "), scheduling job at deadline.");
        } else {
            Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getDeadline());
            if (leastExpensiveHour != null) {
                carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
            }
        }
    }

    @VisibleFor("testing")
    boolean waitJobIfDayAvailableAndDataNotAvailable(DayOfWeek dayOfWeek, Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        if (dayAheadEnergyPrices == null || dayAheadEnergyPrices.getHourlyEnergyPrices() == null || dayAheadEnergyPrices.getHourlyEnergyPrices().isEmpty()) {
            LOGGER.warn("No day ahead energy prices available, keep waiting");
            return true;
        }
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime upcomingDay = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).withHour(15).withMinute(0).withSecond(0).withNano(0);
        Instant upcomingDayAt15 = upcomingDay.toInstant(ZoneOffset.UTC);

        if (isDayBetweenNowAndEnd(carbonAwareAwaitingState.getDeadline(), dayOfWeek)
                && dayAheadEnergyPrices.getMaxHour().isBefore(upcomingDayAt15)) {
            LOGGER.warn("Job {} has a deadline on {}, keep waiting", job.getId(), dayOfWeek);
            return true; // keep waiting
        }
        return false; // schedule job
    }

    @VisibleFor("testing")
    static boolean isDayBetweenNowAndEnd(Instant end, DayOfWeek dayOfWeek) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime endTime = ZonedDateTime.ofInstant(end, ZoneId.systemDefault());

        if (now.isAfter(endTime)) {
            return false; // end is in the past
        }

        // Check each day between now and end
        while (now.isBefore(endTime)) {
            if (now.getDayOfWeek() == dayOfWeek) {
                return true;
            }
            now = now.plusDays(1);
        }

        return false;
    }
}
