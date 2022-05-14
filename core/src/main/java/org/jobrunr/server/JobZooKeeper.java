package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilterUtils;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.server.ZooKeeperRunManager.PreviousRunNotFinishedException;
import org.jobrunr.server.ZooKeeperRunManager.RunTracker;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.storage.*;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final StorageProvider storageProvider;
    private final ZooKeeperRunManager zooKeeperRunManager;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final JobFilterUtils jobFilterUtils;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final ConcurrentJobModificationResolver concurrentJobModificationResolver;
    private final Map<Job, Thread> currentlyProcessedJobs;
    private final AtomicInteger exceptionCount;
    private final ReentrantLock reentrantLock;
    private final AtomicInteger occupiedWorkers;
    private final Duration durationPollIntervalTimeBox;
    private RecurringJobsResult recurringJobs;
    private Instant runStartTime;

    public JobZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.zooKeeperRunManager = new ZooKeeperRunManager(backgroundJobServer, LOGGER);
        this.recurringJobs = new RecurringJobsResult();
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
        if (backgroundJobServer.isUnAnnounced()) return;

        try (RunTracker tracker = zooKeeperRunManager.startRun()) {
            this.runStartTime = tracker.getRunStartTime();
            updateJobsThatAreBeingProcessed();
            runMasterTasksIfCurrentServerIsMaster();
            onboardNewWorkIfPossible();
            this.runStartTime = null;
        } catch (PreviousRunNotFinishedException e) {
            LOGGER.error("JobRunr is passing the poll interval in seconds time-box. This means your poll interval in seconds setting is too small. This can result in an unstable cluster or recurring jobs that are skipped.");
            dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification((long)backgroundJobServerStatus().getPollIntervalInSeconds()));
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
        final int pageRequestSize = backgroundJobServer.getConfiguration().scheduledJobsRequestSize;
        Supplier<List<Job>> scheduledJobsSupplier = () -> storageProvider.getScheduledJobs(now().plusSeconds(backgroundJobServerStatus().getPollIntervalInSeconds()), ascOnUpdatedAt(pageRequestSize));
        processJobList(scheduledJobsSupplier, Job::enqueue);
    }

    void checkForOrphanedJobs() {
        LOGGER.debug("Looking for orphan jobs... ");
        final int pageRequestSize = backgroundJobServer.getConfiguration().orphanedJobsRequestSize;
        final Instant updatedBefore = runStartTime.minus(ofSeconds(backgroundJobServer.getServerStatus().getPollIntervalInSeconds()).multipliedBy(4));
        Supplier<List<Job>> orphanedJobsSupplier = () -> storageProvider.getJobs(PROCESSING, updatedBefore, ascOnUpdatedAt(pageRequestSize));
        processJobList(orphanedJobsSupplier, job -> job.failed("Orphaned job", new IllegalThreadStateException("Job was too long in PROCESSING state without being updated.")));
    }

    void checkForSucceededJobsThanCanGoToDeletedState() {
        LOGGER.debug("Looking for succeeded jobs that can go to the deleted state... ");
        AtomicInteger succeededJobsCounter = new AtomicInteger();

        final int pageRequestSize = backgroundJobServer.getConfiguration().succeededJobsRequestSize;
        final Instant updatedBefore = now().minus(backgroundJobServer.getServerStatus().getDeleteSucceededJobsAfter());
        Supplier<List<Job>> succeededJobsSupplier = () -> storageProvider.getJobs(SUCCEEDED, updatedBefore, ascOnUpdatedAt(pageRequestSize));
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
        storageProvider.deleteJobsPermanently(StateName.DELETED, now().minus(backgroundJobServer.getServerStatus().getPermanentlyDeleteDeletedJobsAfter()));
    }

    void onboardNewWorkIfPossible() {
        if (pollIntervalInSecondsTimeBoxIsAboutToPass()) return;
        if (canOnboardNewWork()) {
            checkForEnqueuedJobs();
        }
    }

    private void showWarningIfPollIntervalInSecondsTimeBoxIsPassed() {
        if(pollIntervalInSecondsTimeBoxIsAboutToPass()) {
            LOGGER.warn("JobRunr is passing the poll interval in seconds time-box. This means your poll interval in seconds setting is too small.");
            dashboardNotificationManager.notify(new PollIntervalInSecondsTimeBoxIsTooSmallNotification((long)backgroundJobServerStatus().getPollIntervalInSeconds()));
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
                .map(RecurringJob::toScheduledJob)
                .collect(toList());
        if (!jobsToSchedule.isEmpty()) {
            storageProvider.save(jobsToSchedule);
        }
    }

    boolean mustSchedule(RecurringJob recurringJob) {
        LOGGER.debug("mustSchedule {}", recurringJob);
        boolean condition1 = recurringJob.getNextRun().isBefore(now().plus(durationPollIntervalTimeBox).plusSeconds(1));
        boolean condition2 = !storageProvider.recurringJobExists(recurringJob.getId(), StateName.SCHEDULED, StateName.ENQUEUED, StateName.PROCESSING);
        boolean flag = condition1 && condition2;
        LOGGER.debug("condition1 {} condition2 {}", condition1, condition2);
        return flag;
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
        return durationRunTime.compareTo(durationPollIntervalTimeBox) >= 0;
    }

    private List<RecurringJob> getRecurringJobs() {
        if (storageProvider.recurringJobsUpdated(recurringJobs.getLastModifiedHash())) {
            this.recurringJobs = storageProvider.getRecurringJobs();
        }
        return this.recurringJobs;
    }

    ConcurrentJobModificationResolver createConcurrentJobModificationResolver() {
        return backgroundJobServer.getConfiguration()
                .concurrentJobModificationPolicy.toConcurrentJobModificationResolver(storageProvider, this);
    }
}
