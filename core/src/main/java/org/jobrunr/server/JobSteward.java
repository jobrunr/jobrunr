package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.tasks.OneOffTaskRunInfo;
import org.jobrunr.server.tasks.steward.OnboardNewWorkTask;
import org.jobrunr.server.tasks.steward.UpdateJobsInProgressTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class JobSteward extends JobHandler implements Runnable {

    static final Logger LOGGER = LoggerFactory.getLogger(JobSteward.class);

    private final Map<Job, Thread> jobsCurrentlyInProgress;
    private final AtomicInteger occupiedWorkers;
    private final OnboardNewWorkTask onboardNewWorkTask;

    public JobSteward(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer,
                new UpdateJobsInProgressTask(backgroundJobServer),
                new OnboardNewWorkTask(backgroundJobServer));
        this.jobsCurrentlyInProgress = new ConcurrentHashMap<>();
        this.occupiedWorkers = new AtomicInteger();
        this.onboardNewWorkTask = getTaskOfType(OnboardNewWorkTask.class);
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }

    void onboardNewWorkIfPossible() {
        onboardNewWorkTask.run(new OneOffTaskRunInfo(backgroundJobServerConfiguration()));
    }

    public void startProcessing(Job job, Thread thread) {
        Optional<Job> optionalExistingThatMayBeReplacedJob = jobsCurrentlyInProgress.keySet().stream().filter(j -> j.getId().equals(job.getId())).findFirst();
        optionalExistingThatMayBeReplacedJob
                .map(j -> j.delete("Job has been replaced"))
                .map(jobsCurrentlyInProgress::get)
                .ifPresent(Thread::interrupt);
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
        onboardNewWorkTask.run(new OneOffTaskRunInfo(backgroundJobServerConfiguration()));
    }
}