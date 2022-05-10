package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.PageRequest;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class JobZooKeeper implements Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final StorageProvider storageProvider;
    private final List<RecurringJob> recurringJobs;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final JobFilterUtils jobFilterUtils;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final ConcurrentJobModificationResolver concurrentJobModificationResolver;
    private final Map<Job, Thread> currentlyProcessedJobs;
    private final AtomicInteger exceptionCount;
    private final ReentrantLock reentrantLock;
    private final AtomicInteger occupiedWorkers;
    private final Duration durationPollIntervalTimeBox;
    private Instant runStartTime;

    public JobZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.recurringJobs = new ArrayList<>();
        this.workDistributionStrategy = backgroundJobServer.getWorkDistributionStrategy();
        this.dashboardNotificationManager = backgroundJobServer.getDashboardNotificationManager();
        this.jobFilterUtils = new JobFilterUtils(backgroundJobServer.getJobFilters());
        this.concurrentJobModificationResolver = createConcurrentJobModificationResolver();
        this.currentlyProcessedJobs = new ConcurrentHashMap<>();
        this.durationPollIntervalTimeBox = Duration.ofSeconds((long) (backgroundJobServerStatus().getPollIntervalInSeconds() - (backgroundJobServerStatus().getPollIntervalInSeconds() * 0.05)));
        this.reentrantLock = new ReentrantLock();
        this.exceptionCount = new AtomicInteger();
        this.occupiedWorkers = new AtomicInteger();
    }

    @Override
    public void run() {
        try {
            runStartTime = Instant.now();
            if (backgroundJobServer.isUnAnnounced()) return;

            updateJobsThatAreBeingProcessed();
            runMasterTasksIfCurrentServerIsMaster();
            onboardNewWorkIfPossible();
        } catch (Exception e) {
            dashboardNotificationManager.handle(e);
            if (exceptionCount.getAndIncrement() < 5) {
                LOGGER.warn(JobRunrException.SHOULD_NOT_HAPPEN_MESSAGE + " - Processing will continue.", e);
            } else {
                LOGGER.error("FATAL - JobRunr encountered too many processing exceptions. Shutting down.", shouldNotHappenException(e));
                backgroundJobServer.stop();
            }
        }
    }

    void updateJobsThatAreBeingProcessed() {
        LOGGER.debug("Updating currently processed jobs... ");
        processJobList(new ArrayList<>(currentlyProcessedJobs.keySet()), this::updateCurrentlyProcessingJob);
    }

    void runMasterTasksIfCurrentServerIsMaster() {
        if (backgroundJobServer.isMaster()) {
            checkForRecurringJobs();
            checkForScheduledJobs();
            checkForOrphanedJobs();
            checkForSucceededJobsThanCanGoToDeletedState();
            checkForJobsThatCanBeDeleted();
        }
    }

    boolean canOnboardNewWork() {
        return backgroundJobServerStatus().isRunning() && workDistributionStrategy.canOnboardNewWork();
    }

    void checkForRecurringJobs() {
        LOGGER.debug("Looking for recurring jobs... ");
        List<RecurringJob> recurringJobs = getRecurringJobs();
        processRecurringJobs(recurringJobs);
    }

    void checkForScheduledJobs() {
        LOGGER.debug("Looking for scheduled jobs... ");
        Supplier<List<Job>> scheduledJobsSupplier = () -> storageProvider.getScheduledJobs(runStartTime.plusSeconds(backgroundJobServerStatus().getPollIntervalInSeconds()), ascOnUpdatedAt(1000));
        processJobList(scheduledJobsSupplier, Job::enqueue);
    }

    void checkForOrphanedJobs() {
        LOGGER.debug("Looking for orphan jobs... ");
        final Instant updatedBefore = runStartTime.minus(ofSeconds(backgroundJobServer.getServerStatus().getPollIntervalInSeconds()).multipliedBy(4));
        Supplier<List<Job>> orphanedJobsSupplier = () -> storageProvider.getJobs(PROCESSING, updatedBefore, ascOnUpdatedAt(1000));
        processJobList(orphanedJobsSupplier, job -> job.failed("Orphaned job", new IllegalThreadStateException("Job was too long in PROCESSING state without being updated.")));
    }

    void checkForSucceededJobsThanCanGoToDeletedState() {
        LOGGER.debug("Looking for succeeded jobs that can go to the deleted state... ");
        AtomicInteger succeededJobsCounter = new AtomicInteger();

        final Instant updatedBefore = runStartTime.minus(backgroundJobServer.getServerStatus().getDeleteSucceededJobsAfter());
        Supplier<List<Job>> succeededJobsSupplier = () -> storageProvider.getJobs(SUCCEEDED, updatedBefore, ascOnUpdatedAt(1000));
        processJobList(succeededJobsSupplier, job -> {
            succeededJobsCounter.incrementAndGet();
            job.delete("JobRunr maintenance - deleting succeeded job");
        });

        if (succeededJobsCounter.get() > 0) {
            storageProvider.publishTotalAmountOfSucceededJobs(succeededJobsCounter.get());
        }
    }

    void checkForJobsThatCanBeDeleted() {
        LOGGER.debug("Looking for deleted jobs that can be deleted permanently... ");
        storageProvider.deleteJobsPermanently(StateName.DELETED, runStartTime.minus(backgroundJobServer.getServerStatus().getPermanentlyDeleteDeletedJobsAfter()));
    }

    void onboardNewWorkIfPossible() {
        if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return;
        if (canOnboardNewWork()) {
            checkForEnqueuedJobs();
        }
    }

    void checkForEnqueuedJobs() {
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

    void processRecurringJobs(List<RecurringJob> recurringJobs) {
        LOGGER.debug("Found {} recurring jobs", recurringJobs.size());
        List<Job> jobsToSchedule = recurringJobs.stream()
                .filter(this::mustSchedule)
                .map(this::toScheduledJob)
                .collect(toList());
        LOGGER.debug("Will schedule {} recurring jobs", jobsToSchedule.size());
        if(!jobsToSchedule.isEmpty()) {
            storageProvider.save(jobsToSchedule);
        }
    }

    void processJobList(Supplier<List<Job>> jobListSupplier, Consumer<Job> jobConsumer) {
        List<Job> jobs = getJobsToProcess(jobListSupplier);
        while (!jobs.isEmpty()) {
            processJobList(jobs, jobConsumer);
            jobs = getJobsToProcess(jobListSupplier);
        }
    }

    void processJobList(List<Job> jobs, Consumer<Job> jobConsumer) {
        if (!jobs.isEmpty()) {
            try {
                jobs.forEach(jobConsumer);
                jobFilterUtils.runOnStateElectionFilter(jobs);
                storageProvider.save(jobs);
                jobFilterUtils.runOnStateAppliedFilters(jobs);
            } catch (ConcurrentJobModificationException concurrentJobModificationException) {
                try {
                    concurrentJobModificationResolver.resolve(concurrentJobModificationException);
                } catch (UnresolvableConcurrentJobModificationException unresolvableConcurrentJobModificationException) {
                    throw new SevereJobRunrException("Could not resolve ConcurrentJobModificationException", unresolvableConcurrentJobModificationException);
                }
            }
        }
    }

    BackgroundJobServerStatus backgroundJobServerStatus() {
        return backgroundJobServer.getServerStatus();
    }

    public void startProcessing(Job job, Thread thread) {
        currentlyProcessedJobs.put(job, thread);
    }

    public void stopProcessing(Job job) {
        currentlyProcessedJobs.remove(job);
    }

    public Thread getThreadProcessingJob(Job job) {
        return currentlyProcessedJobs.get(job);
    }

    public int getOccupiedWorkerCount() {
        return occupiedWorkers.get();
    }

    public void notifyThreadOccupied() {
        occupiedWorkers.incrementAndGet();
    }

    public void notifyThreadIdle() {
        this.occupiedWorkers.decrementAndGet();
        if (workDistributionStrategy.canOnboardNewWork()) {
            checkForEnqueuedJobs();
        }
    }

    private List<Job> getJobsToProcess(Supplier<List<Job>> jobListSupplier) {
        if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return emptyList();
        return jobListSupplier.get();
    }

    private void updateCurrentlyProcessingJob(Job job) {
        try {
            job.updateProcessing();
        } catch (ClassCastException e) {
            // why: because of thread context switching there is a tiny chance that the job has succeeded
        }
    }

    private boolean pollIntervalInSecondsTimeBoxIsAboutToPass() {
        final Duration durationRunTime = Duration.between(runStartTime, now());
        final boolean runTimeBoxIsPassed = durationRunTime.compareTo(durationPollIntervalTimeBox) >= 0;
        if (runTimeBoxIsPassed) {
            LOGGER.debug("JobRunr is passing the poll interval in seconds timebox because of too many tasks.");
        }
        return runTimeBoxIsPassed;
    }

    private List<RecurringJob> getRecurringJobs() {
        if(this.recurringJobs.size() != storageProvider.countRecurringJobs()) {
            this.recurringJobs.clear();
            this.recurringJobs.addAll(storageProvider.getRecurringJobs());
        }
        return this.recurringJobs;
    }

    boolean mustSchedule(RecurringJob recurringJob) {
        Instant nextRun = recurringJob.getNextRun(runStartTime);
        Instant upUntil = runStartTime.plus(durationPollIntervalTimeBox).plusSeconds(1);
        boolean isNextRunInCurrentInterval = nextRun.isBefore(upUntil);
        if(isNextRunInCurrentInterval) {
            boolean recurringJobNotYetRunning = !storageProvider.recurringJobExists(recurringJob.getId(), StateName.SCHEDULED, StateName.ENQUEUED, PROCESSING);
            LOGGER.debug("{} schedule job {} (nextRun={}; takingJobsUntil={}; recurringJobAlreadyRunning={})", recurringJobNotYetRunning ? "Will " : "Will NOT", recurringJob.getId(), nextRun, upUntil, recurringJobNotYetRunning);
            return recurringJobNotYetRunning;
        } else {
            LOGGER.debug("Will NOT schedule job {} (nextRun={}; takingJobsUntil={}; recurringJobAlreadyRunning=NA)", recurringJob.getId(), nextRun, upUntil);
            return false;
        }
    }

    Job toScheduledJob(RecurringJob recurringJob) {
        return recurringJob.toScheduledJob(runStartTime);
    }

    ConcurrentJobModificationResolver createConcurrentJobModificationResolver() {
        return backgroundJobServer.getConfiguration()
                .concurrentJobModificationPolicy.toConcurrentJobModificationResolver(storageProvider, this);
    }
}
