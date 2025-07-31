package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.CarbonAwareAwaitingState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareJobProcessingConfigurationReader;
import org.jobrunr.server.carbonaware.CarbonIntensityApiClient;
import org.jobrunr.server.carbonaware.CarbonIntensityForecast;
import org.jobrunr.server.dashboard.CarbonIntensityApiErrorNotification;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.utils.annotations.VisibleFor;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import static java.lang.String.format;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.LongStream.range;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnScheduledAt;
import static org.jobrunr.utils.InstantUtils.isInstantBeforeOrEqualTo;

public class ProcessCarbonAwareAwaitingJobsTask extends AbstractJobZooKeeperTask {

    private static final int DEFAULT_REFRESH_TIME = 19;

    private final Duration randomRefreshTimeOffset = Duration.ofMinutes(30).plusSeconds(ThreadLocalRandom.current().nextInt(-300, 300)); // why: 30 minutes plus or min 5 min to make sure they don't hammer the server

    private final CarbonAwareJobProcessingConfigurationReader carbonAwareJobProcessingConfiguration;
    private final CarbonIntensityApiClient carbonIntensityApiClient;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final int pageRequestSize;
    private CarbonIntensityForecast carbonIntensityForecast;
    private Instant nextRefreshTime;
    private Instant nextRunTaskTime;

    public ProcessCarbonAwareAwaitingJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.carbonAwareJobProcessingConfiguration = getCarbonAwareJobProcessingConfiguration(backgroundJobServer);
        this.carbonIntensityApiClient = getCarbonIntensityApiClient(backgroundJobServer);
        this.dashboardNotificationManager = getDashboardNotificationManager(backgroundJobServer);
        this.pageRequestSize = backgroundJobServer.getConfiguration().getCarbonAwareAwaitingJobsRequestSize();
        this.carbonIntensityForecast = new CarbonIntensityForecast();
        this.nextRefreshTime = now();
        this.nextRunTaskTime = now();
    }

    @Override
    protected void runTask() {
        if (isInstantBeforeOrEqualTo(nextRunTaskTime, runStartTime())) {
            updateCarbonIntensityForecastIfNecessary();
            processManyJobs(this::getCarbonAwareAwaitingJobs,
                    this::moveCarbonAwareJobToNextState,
                    amountProcessed -> LOGGER.debug("Moved {} carbon aware jobs to next state", amountProcessed));

            nextRunTaskTime = nextRunTaskTime.plus(backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingPollInterval());
        }
    }

    private List<Job> getCarbonAwareAwaitingJobs(List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getCarbonAwareJobList(getDeadlineBeforeWhichToQueryCarbonAwareJobs(), ascOnScheduledAt(pageRequestSize));
    }

    private Instant getDeadlineBeforeWhichToQueryCarbonAwareJobs() {
        return getAvailableForecastEndTime().minusNanos(1);
    }

    private void updateCarbonIntensityForecastIfNecessary() {
        if (isCarbonAwareJobProcessingDisabled()) return;

        if (isInstantBeforeOrEqualTo(nextRefreshTime, runStartTime())) {
            LOGGER.trace("Updating carbon intensity forecast.");
            updateCarbonIntensityForecast();
            updateNextRefreshTime();
            LOGGER.trace("Carbon intensity forecast updated. Next update is planned for {}", nextRefreshTime);
            if (carbonIntensityForecast.hasError()) {
                dashboardNotificationManager.notify(new CarbonIntensityApiErrorNotification(carbonIntensityForecast.getApiResponseStatus()));
            }
        }
    }

    @VisibleFor("testing")
    Instant getAvailableForecastEndTime() {
        if (isCarbonAwareJobProcessingDisabled()) return getDefaultDailyRefreshTime().plus(1, DAYS).plus(randomRefreshTimeOffset);

        Instant forecastEndPeriod = carbonIntensityForecast.getForecastEndPeriod();
        return (forecastEndPeriod != null && forecastEndPeriod.isAfter(nextRefreshTime)) ? forecastEndPeriod : nextRefreshTime;
    }

    @VisibleFor("testing")
    void moveCarbonAwareJobToNextState(Job job) {
        if (!(job.getJobState() instanceof CarbonAwareAwaitingState)) {
            throw new IllegalStateException("Only jobs in CarbonAwaitingState can move to a next state");
        }

        CarbonAwareAwaitingState state = job.getJobState();
        LOGGER.trace("Determining the best moment to schedule Job(id={}, jobName='{}') to minimize carbon impact", job.getId(), job.getJobName());
        try {
            if (isCarbonAwareJobProcessingDisabled()) {
                scheduleJobAt(job, state.getFallbackInstant(), state, "Carbon aware scheduling is disabled, scheduling job at " + state.getFallbackInstant());
            } else if (isDeadlinePassed(state)) {
                scheduleJobAt(job, state.getFallbackInstant(), state, "Passed its deadline, scheduling immediately.");
            } else if (hasTooSmallScheduleMargin(state)) {
                scheduleJobAt(job, state.getFallbackInstant(), state, "Not enough margin (" + state.getMarginDuration() + ") to be scheduled carbon aware.");
            } else if (carbonIntensityForecast.hasNoForecastForPeriod(state.getFrom(), state.getTo())) {
                scheduleJobAt(job, state.getFallbackInstant(), state, getReasonForMissingForecast(state));
            } else {
                scheduleJobAt(job, idealMoment(state), state, "At the best moment to minimize carbon impact in " + this.carbonIntensityForecast.getDisplayName());
            }
        } catch (Exception e) {
            LOGGER.error("Error trying to move the carbon aware job to next state", e);
            scheduleJobAt(job, state.getFallbackInstant(), state, "Unexpected problem scheduling the carbon aware job, scheduling at " + state.getFallbackInstant());
        }
    }

    private boolean isCarbonAwareJobProcessingEnabled() {
        return carbonAwareJobProcessingConfiguration.isEnabled();
    }

    private boolean isCarbonAwareJobProcessingDisabled() {
        return !isCarbonAwareJobProcessingEnabled();
    }

    private String getReasonForMissingForecast(CarbonAwareAwaitingState state) {
        if (carbonIntensityForecast.hasError()) {
            // Do not add error information here to reduce clutter; will be shown in full in the notification centre.
            return "Error retrieving the carbon intensity forecast. The job will be scheduled at the preferred instant or immediately.";
        }
        return format("No carbon intensity forecast available for region %s at period %s - %s. The job will be scheduled at the preferred instant or immediately.", carbonAwareJobProcessingConfiguration.getAreaCode(), state.getFrom(), state.getTo());
    }

    private void scheduleJobAt(Job job, Instant scheduleAt, CarbonAwareAwaitingState state, String reason) {
        state.moveToNextState(job, scheduleAt, reason);
    }

    private boolean isDeadlinePassed(CarbonAwareAwaitingState state) {
        return now().isAfter(state.getTo());
    }

    private boolean hasTooSmallScheduleMargin(CarbonAwareAwaitingState state) {
        if (carbonIntensityForecast.hasNoForecast()) return false;
        Duration margin = state.getMarginDuration();
        return margin.compareTo(carbonIntensityForecast.getForecastInterval().multipliedBy(2)) < 0;
    }

    @VisibleFor("testing")
    void updateCarbonIntensityForecast() {
        CarbonIntensityForecast carbonIntensityForecast = carbonIntensityApiClient.fetchCarbonIntensityForecast();
        if (carbonIntensityForecast.hasNoForecast() && !carbonIntensityForecast.hasError()) {
            LOGGER.warn("No new carbon intensity forecast available. Keeping the old forecast.");
            return;
        }
        this.carbonIntensityForecast = carbonIntensityForecast;
        updateForecastInStorageProvider(carbonIntensityForecast);
    }

    private void updateForecastInStorageProvider(CarbonIntensityForecast carbonIntensityForecast) {
        if (carbonIntensityForecast.getForecastEndPeriod() == null) return;

        List<JobRunrMetadata> allForecasts = this.storageProvider.getMetadata("carbon-intensity-forecast");
        deleteOldUnnecessaryForecasts(allForecasts);

        LocalDate startPeriodOwner = carbonIntensityForecast.getForecastStartPeriod().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endPeriodOwner = carbonIntensityForecast.getForecastEndPeriod().atZone(ZoneOffset.UTC).toLocalDate();
        Predicate<String> hasNoDataForDate = dateAsString -> allForecasts.stream().noneMatch(m -> m.getOwner().equals(dateAsString));
        range(0, DAYS.between(startPeriodOwner, endPeriodOwner) + 1)
                .mapToObj(l -> startPeriodOwner.plusDays(l).toString())
                .distinct()
                .filter(hasNoDataForDate)
                .forEach(owner -> storageProvider.saveMetadata(new JobRunrMetadata("carbon-intensity-forecast", owner, backgroundJobServer.getJsonMapper().serialize(carbonIntensityForecast))));
    }

    private void deleteOldUnnecessaryForecasts(List<JobRunrMetadata> allForecasts) {
        Duration jobCompletelyDeletedAfter = backgroundJobServer.getConfiguration().getDeleteSucceededJobsAfter().plus(backgroundJobServer.getConfiguration().getPermanentlyDeleteDeletedJobsAfter());
        String deleteForecastAfter = now().minus(jobCompletelyDeletedAfter).atZone(ZoneOffset.UTC).toLocalDate().toString();
        allForecasts.stream()
                .filter(metadata -> metadata.getOwner().compareTo(deleteForecastAfter) < 0)
                .forEach(metadata -> storageProvider.deleteMetadata(metadata.getName(), metadata.getOwner()));
    }

    private void updateNextRefreshTime() {
        Instant defaultDailyRefreshTime = getDefaultDailyRefreshTime();
        Instant proposedNextRefreshTime = ofNullable(carbonIntensityForecast.getNextForecastAvailableAt()).orElse(defaultDailyRefreshTime);

        if (proposedNextRefreshTime.isBefore(now())) {
            nextRefreshTime = defaultDailyRefreshTime.atZone(getTimeZone()).getHour() >= DEFAULT_REFRESH_TIME
                    ? defaultDailyRefreshTime.plus(1, DAYS).plus(randomRefreshTimeOffset)
                    : defaultDailyRefreshTime.plus(randomRefreshTimeOffset);
        } else {
            nextRefreshTime = proposedNextRefreshTime.plus(randomRefreshTimeOffset);
        }
    }

    private Instant idealMoment(CarbonAwareAwaitingState carbonAwareAwaitingState) {
        return carbonIntensityForecast.lowestCarbonIntensityInstant(carbonAwareAwaitingState.getFrom(), carbonAwareAwaitingState.getTo());
    }

    @VisibleFor("testing")
    ZoneId getTimeZone() {
        return carbonIntensityForecast.getTimezone() != null
                ? ZoneId.of(carbonIntensityForecast.getTimezone())
                : ZoneId.systemDefault();
    }

    private Instant getDefaultDailyRefreshTime() {
        return ZonedDateTime.now(getTimeZone())
                .truncatedTo(HOURS)
                .withHour(DEFAULT_REFRESH_TIME).toInstant();
    }

    @VisibleFor("testing")
    CarbonAwareJobProcessingConfigurationReader getCarbonAwareJobProcessingConfiguration(BackgroundJobServer backgroundJobServer) {
        return backgroundJobServer.getConfiguration().getCarbonAwareJobProcessingConfiguration();
    }

    @VisibleFor("testing")
    CarbonIntensityApiClient getCarbonIntensityApiClient(BackgroundJobServer backgroundJobServer) {
        return new CarbonIntensityApiClient(carbonAwareJobProcessingConfiguration, backgroundJobServer.getJsonMapper());
    }

    private DashboardNotificationManager getDashboardNotificationManager(BackgroundJobServer backgroundJobServer) {
        return backgroundJobServer.getDashboardNotificationManager();
    }
}
