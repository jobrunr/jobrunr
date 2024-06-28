package org.jobrunr.carbonaware;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Random;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;

// TODO the only times data is fetched is at instantiation or by the Zookeeper Task
// TODO Use system timezone as default timezone; use timezone from day ahead prices if available
public class CarbonAwareJobManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarbonAwareJobManager.class);

    private final CarbonAwareConfigurationReader carbonAwareConfiguration;
    private final CarbonAwareApiClient carbonAwareApiClient;
    private final Random rand = new Random();
    private DayAheadEnergyPrices dayAheadEnergyPrices;

    public CarbonAwareJobManager(CarbonAwareConfiguration carbonAwareConfiguration, JsonMapper jsonMapper) {
        this.carbonAwareConfiguration = new CarbonAwareConfigurationReader(carbonAwareConfiguration);
        this.carbonAwareApiClient = createCarbonAwareApiClient(this.carbonAwareConfiguration, jsonMapper);
        this.dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();
    }

    public CarbonAwareConfigurationReader getCarbonAwareConfiguration() {
        return carbonAwareConfiguration;
    }

    public Instant getZookeeperTaskDailyRunTime(int baseHour) { // why: don't send all the requests to carbon-api at the same moment. Distribute api-calls in a 1-hour period, in 5 sec buckets
        int randomOffsetToDistributeLoadOnApi = rand.nextInt(721) * 5;
        ZonedDateTime baseHourTodayUTC = ZonedDateTime.now(ZoneId.of("Europe/Brussels"))
                .withHour(baseHour).withMinute(0).withSecond(0)
                .plusSeconds(randomOffsetToDistributeLoadOnApi);
        return baseHourTodayUTC.toInstant();
    }

    public void updateDayAheadEnergyPrices() {
        DayAheadEnergyPrices dayAheadEnergyPrices = carbonAwareApiClient.fetchLatestDayAheadEnergyPrices();
        if (dayAheadEnergyPrices.hasNoData()) {
            LOGGER.warn("No new day ahead energy prices available. Keeping the old data.");
            return;
        }
        this.dayAheadEnergyPrices = dayAheadEnergyPrices;
    }

    public String getTimeZone() {
        return Objects.isNull(dayAheadEnergyPrices.getTimezone()) ? "Europe/Brussels" : dayAheadEnergyPrices.getTimezone();
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
        ZonedDateTime nowCET = ZonedDateTime.now(ZoneId.of("Europe/Brussels"));
        LocalDate today = nowCET.toLocalDate();
        LocalDate deadlineDay = carbonAwareAwaitingState.getTo().atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
        boolean todayIsDeadline = today.equals(deadlineDay);
        boolean todayIsPreviousDayBeforeDeadline = today.equals(deadlineDay.minusDays(1));
        boolean timeIsAfter18 = nowCET.getHour() >= 18;
        boolean todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18 = todayIsPreviousDayBeforeDeadline && timeIsAfter18;
        boolean shouldScheduleNow = todayIsDeadline || todayIsPreviousDayBeforeDeadlineAndTimeIsAfter18;
        return !shouldScheduleNow;
    }

    private void scheduleJobAtOptimalTime(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState) {
        Instant leastExpensiveHour = dayAheadEnergyPrices.leastExpensiveHour(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
        if (leastExpensiveHour != null) {
            carbonAwareAwaitingState.moveToNextState(job, leastExpensiveHour);
        }
        LOGGER.trace("No hour found between {} and {} and greater or equal to current hour. Keep waiting.", carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    private CarbonAwareApiClient createCarbonAwareApiClient(CarbonAwareConfigurationReader carbonAwareConfiguration, JsonMapper jsonMapper) {
        return new CarbonAwareApiClient(carbonAwareConfiguration, jsonMapper);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return now().isAfter(carbonAwareAwaitingState.getTo());
    }

    private boolean isDeadlineNextHour(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return now().isAfter(carbonAwareAwaitingState.getTo().minus(1, HOURS));
    }

    private void scheduleJobAtPreferredInstant(Job job, CarbonAwareAwaitingState carbonAwareAwaitingState, String reason) {
        Instant scheduleAt = Objects.isNull(carbonAwareAwaitingState.getPreferredInstant()) ? carbonAwareAwaitingState.getFrom() : carbonAwareAwaitingState.getPreferredInstant();
        carbonAwareAwaitingState.moveToNextState(job, scheduleAt, reason);
    }
}
