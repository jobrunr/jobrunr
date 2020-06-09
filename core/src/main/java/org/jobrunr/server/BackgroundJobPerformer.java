package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.jobrunr.utils.exceptions.Exceptions.hasCause;

public class BackgroundJobPerformer extends AbstractBackgroundJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobPerformer.class);

    private static final AtomicInteger concurrentModificationExceptionCounter = new AtomicInteger();

    public BackgroundJobPerformer(BackgroundJobServer backgroundJobServer, Job job) {
        super(backgroundJobServer, job);
    }

    public void run() {
        boolean canProcess = updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart();
        if (canProcess) {
            try {
                runActualJob();
                updateJobStateToSucceededAndRunJobFilters();
            } catch (Exception e) {
                if (isJobDeletedWhileProcessing(e)) {
                    return;
                } else if (isJobServerStopped(e)) {
                    updateJobStateToFailedAndRunJobFilters("Job processing was stopped as background job server has stopped", e);
                    Thread.currentThread().interrupt();
                } else {
                    updateJobStateToFailedAndRunJobFilters("An exception occurred during the performance of the job", e);
                }
            }
        }
        backgroundJobServer.getJobZooKeeper().notifyThreadIdle();
    }


    private boolean updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart() {
        try {
            job.startProcessingOn(backgroundJobServer);
            saveAndRunStateRelatedJobFilters(job);
            LOGGER.debug("Job {} - {} - processing started", job.getId(), job.getJobName());
            return true;
        } catch (ConcurrentJobModificationException e) {
            // processing already started on other server
            LOGGER.trace("Could not start processing job {} - it is already in a newer state (collision {})", job.getId(), concurrentModificationExceptionCounter.incrementAndGet());
            return false;
        }
    }

    private void runActualJob() throws Exception {
        try {
            backgroundJobServer.getJobZooKeeper().startProcessing(job, Thread.currentThread());
            LOGGER.trace("Job {} is running", job.getId());
            jobFilters.runOnJobProcessingFilters(job);
            BackgroundJobRunner backgroundJobRunner = backgroundJobServer.getBackgroundJobRunner(job);
            backgroundJobRunner.run(job);
            jobFilters.runOnJobProcessedFilters(job);
        } finally {
            backgroundJobServer.getJobZooKeeper().stopProcessing(job);
        }
    }

    private void updateJobStateToSucceededAndRunJobFilters() {
        try {
            job.succeeded();
            saveAndRunStateRelatedJobFilters(job);
            LOGGER.debug("Job {} - {} - processing succeeded", job.getId(), job.getJobName());
        } catch (Exception badException) {
            LOGGER.error("FATAL - could not update job {} to SUCCEEDED state", job.getId(), badException);
        }
    }

    private void updateJobStateToFailedAndRunJobFilters(String message, Exception e) {
        try {
            job.failed(message, e);
            saveAndRunStateRelatedJobFilters(job);
            LOGGER.warn("Job {} - {} - processing failed", job.getId(), job.getJobName(), e);
        } catch (Exception badException) {
            LOGGER.error("FATAL - could not update job {} to FAILED state", job.getId(), badException);
        }
    }

    private boolean isJobDeletedWhileProcessing(Exception e) {
        return hasCause(e, InterruptedException.class) && job.hasState(StateName.DELETED);
    }

    private boolean isJobServerStopped(Exception e) {
        return hasCause(e, InterruptedException.class) && !job.hasState(StateName.DELETED);
    }
}
