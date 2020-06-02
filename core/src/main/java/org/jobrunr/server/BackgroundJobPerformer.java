package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class BackgroundJobPerformer extends AbstractBackgroundJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobPerformer.class);

    private static final AtomicInteger atomicInteger = new AtomicInteger();

    public BackgroundJobPerformer(BackgroundJobServer backgroundJobServer, Job job) {
        super(backgroundJobServer, job);
    }

    @Override
    public Job call() {
        boolean canProcess = updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart();
        if (canProcess) {
            backgroundJobServer.getJobZooKeeper().startProcessing(job);
            try {
                runActualJob();
                updateJobStateToSucceededAndRunJobFilters();
            } catch (InterruptedException e) {
                updateJobStateToFailedAndRunJobFilters("Job processing was stopped externally", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                updateJobStateToFailedAndRunJobFilters("An exception occurred during the performance of the job", e);
            }
            backgroundJobServer.getJobZooKeeper().stopProcessing(job);
        }
        return job;
    }

    private boolean updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart() {
        try {
            job.startProcessingOn(backgroundJobServer);
            saveAndRunStateRelatedJobFilters(job);
            LOGGER.info("Job {} - {} - processing started", job.getId(), job.getJobName());
            return true;
        } catch (ConcurrentJobModificationException e) {
            LOGGER.trace("Could not start processing job {} - it is already in a newer state (collision {})", job.getId(), atomicInteger.incrementAndGet(), e);
            // processing already started on other server
            return false;
        }
    }

    private void runActualJob() throws Exception {
        LOGGER.trace("Job {} is running", job.getId());
        jobFilters.runOnJobProcessingFilters(job);
        BackgroundJobRunner backgroundJobRunner = backgroundJobServer.getBackgroundJobRunner(job);
        backgroundJobRunner.run(job);
        jobFilters.runOnJobProcessedFilters(job);
    }

    private void updateJobStateToSucceededAndRunJobFilters() {
        job.succeeded();
        saveAndRunStateRelatedJobFilters(job);
        LOGGER.info("Job {} - {} - processing succeeded", job.getId(), job.getJobName());
    }

    private void updateJobStateToFailedAndRunJobFilters(String message, Exception e) {
        job.failed(message, e);
        saveAndRunStateRelatedJobFilters(job);
        LOGGER.warn("Job {} - {} - processing failed", job.getId(), job.getJobName(), e);
    }

}
