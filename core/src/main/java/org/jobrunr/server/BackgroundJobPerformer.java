package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.jobrunr.jobs.filters.JobPerformingFilters;
import org.jobrunr.jobs.mappers.MDCMapper;
import org.jobrunr.jobs.states.IllegalJobStateChangeException;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.exceptions.JobNotFoundException;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.utils.annotations.VisibleFor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.utils.exceptions.Exceptions.hasCause;

public class BackgroundJobPerformer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobPerformer.class);

    private static final AtomicInteger concurrentModificationExceptionCounter = new AtomicInteger();
    private final BackgroundJobServer backgroundJobServer;
    private final JobPerformingFilters jobPerformingFilters;
    private final Job job;

    public BackgroundJobPerformer(BackgroundJobServer backgroundJobServer, Job job) {
        this.backgroundJobServer = backgroundJobServer;
        this.jobPerformingFilters = new JobPerformingFilters(job, backgroundJobServer.getJobFilters());
        this.job = job;
    }

    @Override
    public void run() {
        try {
            backgroundJobServer.getJobSteward().notifyThreadOccupied();
            MDCMapper.loadMDCContextFromJob(job);
            performJob();
        } catch (Exception e) {
            if (isJobDeletedWhileProcessing(e)) {
                // nothing to do anymore as Job is deleted
                return;
            } else if (isJobServerStopped(e)) {
                updateJobStateToFailedAndRunJobFilters("Job processing was stopped as background job server has stopped", e);
                Thread.currentThread().interrupt();
            } else if (isJobNotFoundException(e)) {
                updateJobStateToFailedAndRunJobFilters("Job method not found", e);
            } else {
                updateJobStateToFailedAndRunJobFilters("An exception occurred during the performance of the job", e);
            }
        } finally {
            backgroundJobServer.getJobSteward().notifyThreadIdle();
            MDC.clear();
        }
    }

    protected void performJob() throws Exception {
        boolean canProcess = updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart();
        if (canProcess) {
            runActualJob();
            updateJobStateToSucceededAndRunJobFilters();
        }
    }

    private boolean updateJobStateToProcessingRunJobFiltersAndReturnIfProcessingCanStart() {
        try {
            if (hasProcessingStateProvidedByStorageProvider()) return true;

            job.startProcessingOn(backgroundJobServer);
            saveAndRunStateRelatedJobFilters(job);
            LOGGER.debug("Job(id={}, jobName='{}') processing started", job.getId(), job.getJobName());
            return job.hasState(PROCESSING);
        } catch (ConcurrentJobModificationException e) {
            // processing already started on other server
            LOGGER.trace("Could not start processing job {} - it is already in a newer state (collision {})", job.getId(), concurrentModificationExceptionCounter.incrementAndGet(), e);
            return false;
        }
    }

    private void runActualJob() throws Exception {
        try {
            JobRunrDashboardLogger.setJob(job);
            backgroundJobServer.getJobSteward().startProcessing(job, Thread.currentThread());
            LOGGER.trace("Job(id={}, jobName='{}') is running", job.getId(), job.getJobName());
            jobPerformingFilters.runOnJobProcessingFilters();
            BackgroundJobRunner backgroundJobRunner = backgroundJobServer.getBackgroundJobRunner(job);
            backgroundJobRunner.run(job);
            jobPerformingFilters.runOnJobProcessingSucceededFilters();
        } catch (Exception e) {
            jobPerformingFilters.runOnJobProcessingFailedFilters(e);
            throw e;
        } finally {
            backgroundJobServer.getJobSteward().stopProcessing(job);
            JobRunrDashboardLogger.clearJob();
        }
    }

    private void updateJobStateToSucceededAndRunJobFilters() {
        try {
            LOGGER.debug("Job(id={}, jobName='{}') processing succeeded", job.getId(), job.getJobName());
            job.succeeded();
            saveAndRunStateRelatedJobFilters(job);
        } catch (IllegalJobStateChangeException ex) {
            if (ex.getFrom() == DELETED) {
                LOGGER.info("Job finished successfully but it was already deleted - ignoring illegal state change from {} to {}", ex.getFrom(), ex.getTo(), ex);
            } else {
                throw ex;
            }
        } catch (Exception badException) {
            LOGGER.error("ERROR - could not update job(id={}, jobName='{}') to SUCCEEDED state", job.getId(), job.getJobName(), badException);
        }
    }

    private void updateJobStateToFailedAndRunJobFilters(String message, Exception e) {
        try {
            Exception actualException = unwrapException(e);
            job.failed(message, actualException);
            saveAndRunStateRelatedJobFilters(job);
            if (job.getState() == FAILED) {
                LOGGER.error("Job(id={}, jobName='{}') processing failed: {}", job.getId(), job.getJobName(), message, actualException);
            } else {
                LOGGER.warn("Job(id={}, jobName='{}') processing failed: {}", job.getId(), job.getJobName(), message, actualException);
            }
        } catch (IllegalJobStateChangeException ex) {
            if (ex.getFrom() == DELETED) {
                LOGGER.info("Job processing failed but it was already deleted - ignoring illegal state change from {} to {}", ex.getFrom(), ex.getTo(), ex);
            } else {
                throw ex;
            }
        } catch (Exception badException) {
            LOGGER.error("ERROR - could not update job(id={}, jobName='{}') to FAILED state", job.getId(), job.getJobName(), badException);
        }
    }

    protected void saveAndRunStateRelatedJobFilters(Job job) {
        jobPerformingFilters.runOnStateElectionFilter();
        this.backgroundJobServer.getStorageProvider().save(job);
        jobPerformingFilters.runOnStateAppliedFilters();
        if (job.getState() == FAILED) {
            jobPerformingFilters.runOnJobFailedAfterRetriesFilters();
        }
    }

    private boolean hasProcessingStateProvidedByStorageProvider() {
        return job.hasState(PROCESSING) && backgroundJobServer.getConfiguration().getId().equals(job.<ProcessingState>getJobState().getServerId());
    }

    private boolean isJobDeletedWhileProcessing(Exception e) {
        return hasCause(e, InterruptedException.class) && job.hasState(StateName.DELETED);
    }

    private boolean isJobServerStopped(Exception e) {
        return hasCause(e, JobActivatorShutdownException.class) || (hasCause(e, InterruptedException.class) && !job.hasState(StateName.DELETED));
    }

    private boolean isJobNotFoundException(Exception e) {
        return e instanceof JobNotFoundException;
    }

    /**
     * JobRunr uses reflection to run jobs. Any error in jobs is wrapped in {@link InvocationTargetException}.
     * Job details shows {@link InvocationTargetException} and its stacktrace on UI
     * with lots of internal details not related to the job.
     * It makes harder for users to read exceptions
     * and leaves less space for the actual errors' stacktraces on UI.
     */
    @VisibleFor("testing")
    static Exception unwrapException(Exception e) {
        if (e instanceof InvocationTargetException && e.getCause() instanceof Exception) {
            return (Exception) e.getCause();
        }

        return e;
    }
}
