package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.zookeeper.ThreadIdleTaskInfo;
import org.jobrunr.server.zookeeper.ZooKeeperRunTaskInfo;
import org.jobrunr.server.zookeeper.ZooKeeperStatistics;
import org.jobrunr.server.zookeeper.tasks.*;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrException.shouldNotHappenException;

public class JobZooKeeper implements Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final Map<Job, Thread> jobsCurrentlyInProgress;
    private final ZooKeeperStatistics zooKeeperStatistics;
    private final AtomicInteger occupiedWorkers;
    private final ZooKeeperTask updateJobsInProgressTask;
    private final ZooKeeperTask onboardNewWorkTask;
    private final List<ZooKeeperTask> masterTasks;

    public JobZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.zooKeeperStatistics = new ZooKeeperStatistics(backgroundJobServer.getDashboardNotificationManager());
        this.updateJobsInProgressTask = new UpdateJobsInProgressTask(this, backgroundJobServer);
        this.onboardNewWorkTask = new OnboardNewWorkTask(this, backgroundJobServer);
        this.masterTasks = asList(
                new ProcessRecurringJobsTask(this, backgroundJobServer),
                new ProcessScheduledJobsTask(this, backgroundJobServer),
                new ProcessOrphanedJobsTask(this, backgroundJobServer),
                new DeleteSucceededJobsTask(this, backgroundJobServer),
                new DeleteDeletedJobsPermanentlyTask(this, backgroundJobServer)
        );
        this.jobsCurrentlyInProgress = new ConcurrentHashMap<>();
        this.occupiedWorkers = new AtomicInteger();
    }

    @Override
    public void run() {
        if (backgroundJobServer.isUnAnnounced()) return;

        try (ZooKeeperRunTaskInfo runInfo = zooKeeperStatistics.startRun(backgroundJobServerStatus())) {
            updateJobsThatAreBeingProcessed(runInfo);
            runMasterTasksIfCurrentServerIsMaster(runInfo);
            onboardNewWorkIfPossible(runInfo);
            runInfo.markRunAsSucceeded();
        } catch (Exception e) {
            zooKeeperStatistics.handleException(e);
            if (zooKeeperStatistics.hasTooManyExceptions()) {
                if (e instanceof StorageException) {
                    LOGGER.error("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/", e);
                } else {
                    LOGGER.error("FATAL - JobRunr encountered too many processing exceptions. Shutting down.", shouldNotHappenException(e));
                }
                backgroundJobServer.stop();
            } else {
                LOGGER.warn(JobRunrException.SHOULD_NOT_HAPPEN_MESSAGE + " - Processing will continue.", e);
            }
        }
    }

    void updateJobsThatAreBeingProcessed(ZooKeeperRunTaskInfo runInfo) {
        updateJobsInProgressTask.run(runInfo);
    }

    void runMasterTasksIfCurrentServerIsMaster(ZooKeeperRunTaskInfo runInfo) {
        if (backgroundJobServer.isMaster()) {
            masterTasks.forEach(task -> task.run(runInfo));
        }
    }

    void onboardNewWorkIfPossible(ZooKeeperRunTaskInfo runInfo) {
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
        onboardNewWorkTask.run(new ThreadIdleTaskInfo(backgroundJobServerStatus()));
    }
}
