package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.server.threadpool.ScheduledThreadPool;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class BackgroundJobServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final BackgroundJobServerStatus serverStatus;
    private final StorageProvider storageProvider;
    private final List<BackgroundJobRunner> backgroundJobRunners;
    private final ServerZooKeeper serverZooKeeper;
    private final JobZooKeeper jobZooKeeper;

    private ScheduledThreadPoolExecutor zookeeperThreadPool;
    private ScheduledThreadPoolExecutor workThreadPool;
    private JobFilters jobFilters;

    public BackgroundJobServer(StorageProvider storageProvider) {
        this(storageProvider, null);
    }


    public BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator) {
        this(storageProvider, jobActivator, 15, defaultThreadPoolSize());
    }

    public BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, int pollIntervalInSeconds, int workerCount) {
        this(storageProvider, jobActivator, new BackgroundJobServerStatus(pollIntervalInSeconds, workerCount));
    }

    public BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, BackgroundJobServerStatus serverStatus) {
        if (storageProvider == null) throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider");

        this.serverStatus = serverStatus;
        this.storageProvider = storageProvider;
        this.backgroundJobRunners = initializeBackgroundJobRunners(jobActivator);
        this.jobFilters = new JobFilters();
        this.serverZooKeeper = new ServerZooKeeper(this);
        this.jobZooKeeper = new JobZooKeeper(this);
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void setJobFilters(List<JobFilter> jobFilters) {
        this.jobFilters = new JobFilters(jobFilters);
    }

    JobFilters getJobFilters() {
        return jobFilters;
    }

    public void start() {
        if (isStarted()) return;
        serverStatus.start();
        startZooKeepers();
        startWorkers();
        LOGGER.info("BackgroundJobServer and BackgroundJobPerformers started successfully");
    }

    public void pauseProcessing() {
        serverStatus.pause();
        stopWorkers();
    }

    public void resumeProcessing() {
        startWorkers();
        serverStatus.resume();
    }

    public void stop() {
        if (isStopped()) return;
        stopWorkers();
        stopZooKeepers();
        serverStatus.stop();
        LOGGER.info("BackgroundJobServer and BackgroundJobPerformers stopped");
    }

    public BackgroundJobServerStatus getServerStatus() {
        return serverStatus;
    }

    public JobZooKeeper getJobZooKeeper() {
        return jobZooKeeper;
    }

    public UUID getId() {
        return serverStatus.getId();
    }

    public int getWorkQueueSize() {
        return workThreadPool.getQueue().size();
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    BackgroundJobRunner getBackgroundJobRunner(Job job) {
        return backgroundJobRunners.stream()
                .filter(jobRunner -> jobRunner.supports(job))
                .findFirst()
                .orElseThrow(() -> JobRunrException.shouldNotHappenException("Could not find a BackgroundJobRunner"));
    }

    void processJob(Job job) {
        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(this, job);
        workThreadPool.submit(backgroundJobPerformer);
        LOGGER.debug("Submitted BackgroundJobPerformer for job {} to executor service", job.getId());
    }

    void processJobs(List<Job> jobs) {
        final List<BackgroundJobPerformer> work = jobs.stream()
                .map(job -> new BackgroundJobPerformer(this, job))
                .collect(Collectors.toList());
        try {
            workThreadPool.invokeAll(work);
            jobZooKeeper.notifyQueueEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    void scheduleJob(RecurringJob recurringJob) {
        Job job = recurringJob.toScheduledJob();
        this.storageProvider.save(job);
    }

    boolean isStarted() {
        return !isStopped();
    }

    boolean isStopped() {
        return zookeeperThreadPool == null;
    }

    private void startZooKeepers() {
        zookeeperThreadPool = new ScheduledThreadPool(2, "backgroundjob-zookeeper-pool");
        zookeeperThreadPool.scheduleAtFixedRate(serverZooKeeper, 0, serverStatus.getPollIntervalInSeconds(), TimeUnit.SECONDS);
        zookeeperThreadPool.scheduleAtFixedRate(jobZooKeeper, 1, serverStatus.getPollIntervalInSeconds(), TimeUnit.SECONDS);
    }

    private void startWorkers() {
        workThreadPool = new ScheduledThreadPool(serverStatus.getWorkerPoolSize(), "backgroundjob-worker-pool");
    }

    private void stopZooKeepers() {
        stop(zookeeperThreadPool);
        this.zookeeperThreadPool = null;
    }

    private void stopWorkers() {
        stop(workThreadPool);
        this.workThreadPool = null;
    }

    private List<BackgroundJobRunner> initializeBackgroundJobRunners(JobActivator jobActivator) {
        return asList(
                new BackgroundJobWithoutIocRunner(),
                new BackgroundStaticJobWithoutIocRunner(),
                new BackgroundJobWithIocRunner(jobActivator)
        );
    }

    private void stop(ScheduledExecutorService executorService) {
        if (executorService == null) return;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // see https://jobs.zalando.com/en/tech/blog/how-to-set-an-ideal-thread-pool-size
    private static int defaultThreadPoolSize() {
        return Runtime.getRuntime().availableProcessors() * 8;
    }

}
