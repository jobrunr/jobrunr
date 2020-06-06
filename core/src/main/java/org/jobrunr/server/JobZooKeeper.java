package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.strategy.BasicWorkDistributionStrategy;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.Boolean.TRUE;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.stream.Collectors.joining;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public class JobZooKeeper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final StorageProvider storageProvider;
    private final JobFilters jobFilters;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final List<Job> currentlyProcessedJobs;
    private final AtomicInteger exceptionCount;
    private volatile Boolean isMaster;
    private final ReentrantLock reentrantLock;

    public JobZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.jobFilters = backgroundJobServer.getJobFilters();
        this.workDistributionStrategy = new BasicWorkDistributionStrategy(backgroundJobServer, this);
        this.currentlyProcessedJobs = new CopyOnWriteArrayList<>();
        this.reentrantLock = new ReentrantLock();
        this.exceptionCount = new AtomicInteger();
    }

    @Override
    public void run() {
        try {
            if (isNotInitialized()) return;

            if (canOnboardNewWork()) {
                if (TRUE.equals(isMaster)) {
                    checkForRecurringJobs();
                    checkForScheduledJobs();
                    checkForOrphanedJobs();
                    checkForSucceededJobsThanCanGoToDeletedState();
                    checkForJobsThatCanBeDeleted();
                }

                updateJobsThatAreBeingProcessed();
                checkForEnqueuedJobs();
            } else {
                updateJobsThatAreBeingProcessed();
            }
        } catch (Exception e) {
            if (exceptionCount.getAndIncrement() < 5) {
                LOGGER.warn(JobRunrException.SHOULD_NOT_HAPPEN_MESSAGE + " - Processing will continue.", e);
            } else {
                LOGGER.error("FATAL - JobRunr encountered too many processing exceptions. Shutting down.", shouldNotHappenException(e));
                backgroundJobServer.stop();
            }
        }
    }

    public void stop() {
        this.isMaster = false;
    }

    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void notifyQueueEmpty() {
        checkForEnqueuedJobs();
    }

    private boolean isNotInitialized() {
        return isMaster == null;
    }

    private boolean canOnboardNewWork() {
        return backgroundJobServerStatus().isRunning() && workDistributionStrategy.canOnboardNewWork();
    }

    private void checkForRecurringJobs() {
        LOGGER.debug("Looking for recurring jobs... ");
        List<RecurringJob> enqueuedJobs = storageProvider.getRecurringJobs();
        processRecurringJobs(enqueuedJobs);
    }

    private void checkForScheduledJobs() {
        LOGGER.debug("Looking for scheduled jobs... ");

        Supplier<List<Job>> scheduledJobsSupplier = () -> storageProvider.getScheduledJobs(now().plusSeconds(backgroundJobServerStatus().getPollIntervalInSeconds()), PageRequest.asc(0, 1000));
        processJobList(scheduledJobsSupplier, Job::enqueue);
    }

    private void checkForOrphanedJobs() {
        LOGGER.debug("Looking for orphan jobs... ");
        final Instant updatedBefore = now().minus(ofSeconds(backgroundJobServer.getServerStatus().getPollIntervalInSeconds()).multipliedBy(4));
        Supplier<List<Job>> orphanedJobsSupplier = () -> storageProvider.getJobs(PROCESSING, updatedBefore, PageRequest.asc(0, 1000));
        processJobList(orphanedJobsSupplier, job -> job.failed("Orphaned job", new IllegalThreadStateException("Job was too long in PROCESSING state without being updated.")));
    }

    private void checkForSucceededJobsThanCanGoToDeletedState() {
        LOGGER.debug("Looking for succeeded jobs that can go to the deleted state... ");
        AtomicInteger succeededJobsCounter = new AtomicInteger();

        final Instant updatedBefore = now().minus(36, ChronoUnit.HOURS);
        Supplier<List<Job>> succeededJobsSupplier = () -> storageProvider.getJobs(SUCCEEDED, updatedBefore, PageRequest.asc(0, 1000));
        processJobList(succeededJobsSupplier, job -> {
            succeededJobsCounter.incrementAndGet();
            job.delete();
        });

        if (succeededJobsCounter.get() > 0) {
            storageProvider.publishJobStatCounter(SUCCEEDED, succeededJobsCounter.get());
        }
    }

    private void checkForJobsThatCanBeDeleted() {
        LOGGER.debug("Looking for deleted jobs that can be deleted permanently... ");
        storageProvider.deleteJobs(StateName.DELETED, now().minus(72, ChronoUnit.HOURS));
    }

    private void updateJobsThatAreBeingProcessed() {
        LOGGER.info("Updating currently processed jobs... ");
        processJobList(new ArrayList<>(currentlyProcessedJobs), Job::updateProcessing, Optional.of(new LogExceptionHandler()));
    }

    private void checkForEnqueuedJobs() {
        try {
            if (reentrantLock.tryLock()) {
                LOGGER.debug("Looking for enqueued jobs... ");
                final PageRequest workPageRequest = workDistributionStrategy.getWorkPageRequest();
                if (workPageRequest.getLimit() > 0) {
                    final List<Job> enqueuedJobs = storageProvider.getJobs(StateName.ENQUEUED, workPageRequest);
                    enqueuedJobs.forEach(backgroundJobServer::processJob);
                }
            }
        } finally {
            if (reentrantLock.isHeldByCurrentThread()) {
                reentrantLock.unlock();
            }
        }
    }

    private void processRecurringJobs(List<RecurringJob> recurringJobs) {
        LOGGER.debug("Found {} recurring jobs", recurringJobs.size());
        recurringJobs.stream()
                .filter(this::isNotYetScheduled)
                .forEach(backgroundJobServer::scheduleJob);
    }

    private boolean isNotYetScheduled(RecurringJob recurringJob) {
        if (storageProvider.exists(recurringJob.getJobDetails(), StateName.SCHEDULED)) return false;
        else if (storageProvider.exists(recurringJob.getJobDetails(), StateName.ENQUEUED)) return false;
        else if (storageProvider.exists(recurringJob.getJobDetails(), StateName.PROCESSING)) return false;
        else return true;
    }

    private void processJobList(Supplier<List<Job>> jobListSupplier, Consumer<Job> jobConsumer) {
        List<Job> jobs = jobListSupplier.get();
        while (!jobs.isEmpty()) {
            processJobList(jobs, jobConsumer);
            jobs = jobListSupplier.get();
        }
    }

    private void processJobList(List<Job> jobs, Consumer<Job> jobConsumer) {
        processJobList(jobs, jobConsumer, Optional.empty());
    }

    private void processJobList(List<Job> jobs, Consumer<Job> jobConsumer, Optional<ExceptionHandler> exceptionHandler) {
        if (!jobs.isEmpty()) {
            try {
                jobs.forEach(jobConsumer);
                jobFilters.runOnStateElectionFilter(jobs);
                storageProvider.save(jobs);
                jobFilters.runOnStateAppliedFilters(jobs);
            } catch (Exception e) {
                if (exceptionHandler.isPresent()) {
                    exceptionHandler.get().handle(e, jobs);
                } else {
                    throw e;
                }
            }
        }
    }

    private BackgroundJobServerStatus backgroundJobServerStatus() {
        return backgroundJobServer.getServerStatus();
    }

    public void startProcessing(Job job) {
        currentlyProcessedJobs.add(job);
    }

    public void stopProcessing(Job job) {
        currentlyProcessedJobs.remove(job);
        if (workDistributionStrategy.canOnboardNewWork()) notifyQueueEmpty();
    }

    public int getWorkQueueSize() {
        return currentlyProcessedJobs.size();
    }

    private static class LogExceptionHandler implements ExceptionHandler {

        @Override
        public void handle(Exception e, List<Job> jobs) {
            LOGGER.error("ProcessJobList FAILED", new JobRunrException("Exception processing one of these jobs: " + jobs.stream().map(n -> n.getId().toString()).collect(joining(",")), e));
        }
    }

    private interface ExceptionHandler {

        void handle(Exception e, List<Job> jobs);
    }
}
