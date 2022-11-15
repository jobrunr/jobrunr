package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.zookeeper.tasks.*;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class JobZooKeeper implements Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final Map<Job, Thread> jobsCurrentlyInProgress;
    private final AtomicInteger exceptionCount;
    private final AtomicInteger occupiedWorkers;
    private final ZooKeeperTask updateJobsInProgressTask;
    private final ZooKeeperTask onboardNewWorkTask;
    private final List<ZooKeeperTask> masterTasks;
    private ZooKeeperRunInfo runInfo;

    public JobZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.updateJobsInProgressTask = new UpdateJobsInProgressTask(this, backgroundJobServer);
        this.onboardNewWorkTask = new OnboardNewWorkTask(this, backgroundJobServer);
        this.masterTasks = Arrays.asList(
                new ProcessRecurringJobsTask(this, backgroundJobServer),
                new ProcessScheduledJobsTask(this, backgroundJobServer),
                new ProcessOrphanedJobsTask(this, backgroundJobServer),
                new DeleteSucceededJobsTask(this, backgroundJobServer),
                new DeleteDeletedJobsPermanentlyTask(this, backgroundJobServer)
        );
        this.dashboardNotificationManager = backgroundJobServer.getDashboardNotificationManager();
        this.jobsCurrentlyInProgress = new ConcurrentHashMap<>();
        this.exceptionCount = new AtomicInteger();
        this.occupiedWorkers = new AtomicInteger();
    }

    @Override
    public void run() {
        try {
            if (backgroundJobServer.isUnAnnounced()) return;
            runInfo = new ZooKeeperRunInfo(backgroundJobServerStatus());

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
        updateJobsInProgressTask.run(runInfo);
    }

    void runMasterTasksIfCurrentServerIsMaster() {
        if (backgroundJobServer.isMaster()) {
            masterTasks.forEach(task -> task.run(runInfo));
        }
    }

    void onboardNewWorkIfPossible() {
        onboardNewWorkTask.run(runInfo);
    }

    BackgroundJobServerStatus backgroundJobServerStatus() {
        return backgroundJobServer.getServerStatus();
    }

    public void startProcessing(Job job, Thread thread) {
        jobsCurrentlyInProgress.put(job, thread);
    }

    public void stopProcessing(Job job) {
        jobsCurrentlyInProgress.remove(job);
    }

    public Set<Job> getJobsInProgress() {
        return jobsCurrentlyInProgress.keySet();
    }

    public Thread getThreadProcessingJob(Job job) {
        return jobsCurrentlyInProgress.get(job);
    }

    public int getOccupiedWorkerCount() {
        return occupiedWorkers.get();
    }

    public void notifyThreadOccupied() {
        occupiedWorkers.incrementAndGet();
    }

    public void notifyThreadIdle() {
        this.occupiedWorkers.decrementAndGet();
        onboardNewWorkTask.run(new ZooKeeperRunInfo(backgroundJobServerStatus()));
    }
}
