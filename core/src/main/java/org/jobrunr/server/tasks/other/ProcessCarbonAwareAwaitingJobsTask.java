package org.jobrunr.server.tasks.other;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.carbonaware.CarbonAwareJobManager;
import org.jobrunr.server.tasks.zookeeper.AbstractJobZooKeeperTask;
import org.jobrunr.storage.JobRunrMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.Collections.emptyList;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnCarbonAwareDeadline;

public class ProcessCarbonAwareAwaitingJobsTask extends AbstractJobZooKeeperTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCarbonAwareAwaitingJobsTask.class);

    private static final int DEFAULT_HOURS_AHEAD_TO_CHECK = 30;
    private final CarbonAwareJobManager carbonAwareJobManager;
    private final int hoursAheadToCheck;
    private final int pageRequestSize;
    private static final String LAST_RUN_METADATA_NAME = "process_carbon_aware_jobs_last_run";

    public ProcessCarbonAwareAwaitingJobsTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.carbonAwareJobManager = CarbonAwareJobManager.getInstance();
        this.pageRequestSize = backgroundJobServer.getConfiguration().getCarbonAwareAwaitingJobsRequestSize();
        hoursAheadToCheck = DEFAULT_HOURS_AHEAD_TO_CHECK;
    }

    @Override
    protected void runTask() {
        Optional<JobRunrMetadata> lastRunMetadata = Optional.ofNullable(storageProvider.getMetadata(LAST_RUN_METADATA_NAME, "cluster"));
        if (lastRunMetadata.isPresent() && hasRunToday(lastRunMetadata.get())) {  // why: this task only has to run once a day, from 1 BackgroundJobServer
            LOGGER.debug("Task ProcessCarbonAwareAwaitingJobsTask already ran today. Skipping.");
            return;
        }
        carbonAwareJobManager.updateDayAheadEnergyPrices();
        processManyJobs(this::getCarbonAwareAwaitingJobs,
                this::moveCarbonAwareJobToNextState,
                amountProcessed -> LOGGER.info("Moved {} carbon aware jobs to next state", amountProcessed));
        storageProvider.saveMetadata(new JobRunrMetadata(LAST_RUN_METADATA_NAME, "cluster", Instant.now().toString()));
    }

    private boolean hasRunToday(JobRunrMetadata lastRunMetadata) {
        try {
            Instant lastRun = Instant.parse(lastRunMetadata.getValue());
            LocalDate lastRunDate = lastRun.atZone(ZoneId.of("Europe/Brussels")).toLocalDate();
            LocalDate today = LocalDate.now(ZoneId.of("Europe/Brussels"));
            return lastRunDate.equals(today);
        } catch (DateTimeParseException e) {
            LOGGER.error("Could not parse last run date from metadata: {}. Assuming task has not run today.", lastRunMetadata.getValue());
            return false;
        }
    }

    private List<Job> getCarbonAwareAwaitingJobs(List<Job> previousResults) {
        if (previousResults != null && previousResults.size() < pageRequestSize) return emptyList();
        return storageProvider.getCarbonAwareJobList(Instant.now().plus(hoursAheadToCheck, HOURS), ascOnCarbonAwareDeadline(pageRequestSize));
    }

    private void moveCarbonAwareJobToNextState(Job job) {
        carbonAwareJobManager.moveToNextState(job);
    }
}
