package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.jobs.filters.JobFilters;
import org.jobrunr.server.jmx.BackgroundJobServerMBean;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.server.threadpool.ScheduledThreadPool;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardConfiguration;

public class BackgroundJobServer implements BackgroundJobServerMBean {

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
        this(storageProvider, jobActivator, usingStandardConfiguration());
    }

    @Deprecated
    /**
     * Use BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, BackgroundJobServerConfiguration configuration)
     */
    public BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, int pollIntervalInSeconds, int workerCount) {
        this(storageProvider, jobActivator, usingStandardConfiguration().andPollIntervalInSeconds(pollIntervalInSeconds).andWorkerCount(workerCount));
    }

    public BackgroundJobServer(StorageProvider storageProvider, JobActivator jobActivator, BackgroundJobServerConfiguration configuration) {
        if (storageProvider == null) throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider");

        this.serverStatus = new BackgroundJobServerStatus(configuration.pollIntervalInSeconds, configuration.backgroundJobServerWorkerPolicy.getWorkerCount());
        this.storageProvider = new ThreadSafeStorageProvider(storageProvider);
        this.backgroundJobRunners = initializeBackgroundJobRunners(jobActivator);
        this.jobFilters = new JobFilters();
        this.serverZooKeeper = createServerZooKeeper();
        this.jobZooKeeper = createJobZooKeeper();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "extShutdownHook"));
    }

    public void setJobFilters(List<JobFilter> jobFilters) {
        this.jobFilters = new JobFilters(jobFilters);
    }

    JobFilters getJobFilters() {
        return jobFilters;
    }

    public boolean isRunning() {
        return serverStatus.isRunning();
    }

    public void start() {
        if (isStarted()) return;
        serverStatus.start();
        startZooKeepers();
        startWorkers();
        LOGGER.info("BackgroundJobServer ({}) and BackgroundJobPerformers started successfully", getId());
    }

    public void pauseProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before pausing");
        if (isPaused()) return;
        serverStatus.pause();
        stopWorkers();
        LOGGER.info("Paused job processing");
    }

    public void resumeProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before resuming");
        if (isProcessing()) return;
        startWorkers();
        serverStatus.resume();
        LOGGER.info("Resumed job processing");
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

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    BackgroundJobRunner getBackgroundJobRunner(Job job) {
        return backgroundJobRunners.stream()
                .filter(jobRunner -> jobRunner.supports(job))
                .findFirst()
                .orElseThrow(() -> problematicConfigurationException("Could not find a BackgroundJobRunner: either no JobActivator is registered, your Background Job Class is not registered within the IoC container or your Job does not have a default no-arg constructor."));
    }

    void processJob(Job job) {
        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(this, job);
        workThreadPool.submit(backgroundJobPerformer);
        LOGGER.debug("Submitted BackgroundJobPerformer for job {} to executor service", job.getId());
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

    boolean isPaused() {
        return !isProcessing();
    }

    boolean isProcessing() {
        return serverStatus.isRunning();
    }

    private void startZooKeepers() {
        zookeeperThreadPool = new ScheduledThreadPool(2, "backgroundjob-zookeeper-pool");
        zookeeperThreadPool.scheduleAtFixedRate(serverZooKeeper, 0, serverStatus.getPollIntervalInSeconds(), TimeUnit.SECONDS);
        zookeeperThreadPool.scheduleAtFixedRate(jobZooKeeper, 1, serverStatus.getPollIntervalInSeconds(), TimeUnit.SECONDS);
    }

    private void startWorkers() {
        workThreadPool = new ScheduledThreadPool(serverStatus.getWorkerPoolSize(), "backgroundjob-worker-pool");
        workThreadPool.prestartAllCoreThreads();
    }

    private void stopZooKeepers() {
        jobZooKeeper.stop();
        serverZooKeeper.stop();
        stop(zookeeperThreadPool);
        this.zookeeperThreadPool = null;
    }

    private void stopWorkers() {
        stop(workThreadPool);
        this.workThreadPool = null;
    }

    private List<BackgroundJobRunner> initializeBackgroundJobRunners(JobActivator jobActivator) {
        return asList(
                new BackgroundJobWithIocRunner(jobActivator),
                new BackgroundJobWithoutIocRunner(),
                new BackgroundStaticJobWithoutIocRunner()
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

    private JobZooKeeper createJobZooKeeper() {
        return new JobZooKeeper(this);
    }

    private ServerZooKeeper createServerZooKeeper() {
        return new ServerZooKeeper(this);
    }

}
