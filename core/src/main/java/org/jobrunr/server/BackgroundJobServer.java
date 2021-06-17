package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.server.jmx.BackgroundJobServerMBean;
import org.jobrunr.server.jmx.JobServerStats;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.tasks.CheckIfAllJobsExistTask;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.ScheduledThreadPoolJobRunrExecutor;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.compare;
import static java.util.Arrays.asList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;

public class BackgroundJobServer implements BackgroundJobServerMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final UUID backgroundJobServerId;
    private final BackgroundJobServerConfiguration configuration;
    private final StorageProvider storageProvider;
    private final JsonMapper jsonMapper;
    private final List<BackgroundJobRunner> backgroundJobRunners;
    private final JobDefaultFilters jobDefaultFilters;
    private final JobServerStats jobServerStats;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final ServerZooKeeper serverZooKeeper;
    private final JobZooKeeper jobZooKeeper;
    private volatile Instant firstHeartbeat;
    private volatile boolean isRunning;
    private volatile Boolean isMaster;

    private ScheduledThreadPoolExecutor zookeeperThreadPool;
    private JobRunrExecutor jobExecutor;

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, null);
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator) {
        this(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration configuration) {
        if (storageProvider == null)
            throw new IllegalArgumentException("A JobStorageProvider is required to use the JobScheduler. Please see the documentation on how to setup a JobStorageProvider");

        this.backgroundJobServerId = UUID.randomUUID();
        this.configuration = configuration;
        this.storageProvider = new ThreadSafeStorageProvider(storageProvider);
        this.jsonMapper = jsonMapper;
        this.backgroundJobRunners = initializeBackgroundJobRunners(jobActivator);
        this.jobDefaultFilters = new JobDefaultFilters();
        this.jobServerStats = new JobServerStats();
        this.workDistributionStrategy = createWorkDistributionStrategy(configuration);
        this.serverZooKeeper = createServerZooKeeper();
        this.jobZooKeeper = createJobZooKeeper();
    }

    public UUID getId() {
        return backgroundJobServerId;
    }

    public synchronized void start() {
        start(true);
    }

    public synchronized void start(boolean guard) {
        if (guard) {
            if (isStarted()) return;
            firstHeartbeat = Instant.now();
            isRunning = true;
            startZooKeepers();
            startWorkers();
            runStartupTasks();
        }
    }

    public synchronized void pauseProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before pausing");
        if (isPaused()) return;
        isRunning = false;
        stopWorkers();
        LOGGER.info("Paused job processing");
    }

    public synchronized void resumeProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before resuming");
        if (isProcessing()) return;
        startWorkers();
        isRunning = true;
        LOGGER.info("Resumed job processing");
    }

    public synchronized void stop() {
        if (isStopped()) return;
        isMaster = null;
        stopWorkers();
        stopZooKeepers();
        isRunning = false;
        firstHeartbeat = null;
        LOGGER.info("BackgroundJobServer and BackgroundJobPerformers stopped");
    }

    public boolean isAnnounced() {
        return isMaster != null;
    }

    public boolean isUnAnnounced() {
        return !isAnnounced();
    }

    public boolean isMaster() {
        return isAnnounced() && isMaster;
    }

    synchronized void setIsMaster(Boolean isMaster) {
        this.isMaster = isMaster;
        if (isMaster != null) {
            LOGGER.info("JobRunr BackgroundJobServer ({}) and {} BackgroundJobPerformers started successfully", getId(), workDistributionStrategy.getWorkerCount());
        } else {
            LOGGER.error("JobRunr BackgroundJobServer failed to start");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public BackgroundJobServerStatus getServerStatus() {
        return new BackgroundJobServerStatus(
                backgroundJobServerId, workDistributionStrategy.getWorkerCount(),
                configuration.pollIntervalInSeconds, configuration.deleteSucceededJobsAfter, configuration.permanentlyDeleteDeletedJobsAfter,
                firstHeartbeat, Instant.now(), isRunning, jobServerStats.getSystemTotalMemory(), jobServerStats.getSystemFreeMemory(),
                jobServerStats.getSystemCpuLoad(), jobServerStats.getProcessMaxMemory(), jobServerStats.getProcessFreeMemory(),
                jobServerStats.getProcessAllocatedMemory(), jobServerStats.getProcessCpuLoad()
        );
    }

    public JobZooKeeper getJobZooKeeper() {
        return jobZooKeeper;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public JsonMapper getJsonMapper() {
        return jsonMapper;
    }

    public WorkDistributionStrategy getWorkDistributionStrategy() {
        return workDistributionStrategy;
    }

    public void setJobFilters(List<JobFilter> jobFilters) {
        this.jobDefaultFilters.addAll(jobFilters);
    }

    JobDefaultFilters getJobFilters() {
        return jobDefaultFilters;
    }

    BackgroundJobRunner getBackgroundJobRunner(Job job) {
        return backgroundJobRunners.stream()
                .filter(jobRunner -> jobRunner.supports(job))
                .findFirst()
                .orElseThrow(() -> problematicConfigurationException("Could not find a BackgroundJobRunner: either no JobActivator is registered, your Background Job Class is not registered within the IoC container or your Job does not have a default no-arg constructor."));
    }

    void processJob(Job job) {
        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(this, job);
        jobExecutor.execute(backgroundJobPerformer);
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
        return isRunning;
    }

    private void startZooKeepers() {
        zookeeperThreadPool = new ScheduledThreadPoolJobRunrExecutor(2, "backgroundjob-zookeeper-pool");
        zookeeperThreadPool.scheduleAtFixedRate(serverZooKeeper, 0, configuration.pollIntervalInSeconds, TimeUnit.SECONDS);
        zookeeperThreadPool.scheduleAtFixedRate(jobZooKeeper, 1, configuration.pollIntervalInSeconds, TimeUnit.SECONDS);
    }

    private void stopZooKeepers() {
        serverZooKeeper.stop();
        stop(zookeeperThreadPool);
        this.zookeeperThreadPool = null;
    }

    private void startWorkers() {
        jobExecutor = loadJobRunrExecutor();
        jobExecutor.start();
    }

    private void stopWorkers() {
        if (jobExecutor == null) return;
        jobExecutor.stop();
        this.jobExecutor = null;
    }

    private void runStartupTasks() {
        try {
            List<Runnable> startupTasks = asList(new CheckIfAllJobsExistTask(this));
            startupTasks.forEach(jobExecutor::execute);
        } catch (Exception notImportant) {
            // server is shut down immediately
        }
    }

    private List<BackgroundJobRunner> initializeBackgroundJobRunners(JobActivator jobActivator) {
        return asList(
                new BackgroundStaticJobWithoutIocRunner(),
                new BackgroundJobWithIocRunner(jobActivator),
                new BackgroundJobWithoutIocRunner()
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

    private ServerZooKeeper createServerZooKeeper() {
        return new ServerZooKeeper(this);
    }

    private JobZooKeeper createJobZooKeeper() {
        return new JobZooKeeper(this);
    }

    private WorkDistributionStrategy createWorkDistributionStrategy(BackgroundJobServerConfiguration configuration) {
        return configuration.backgroundJobServerWorkerPolicy.toWorkDistributionStrategy(this);
    }

    private JobRunrExecutor loadJobRunrExecutor() {
        ServiceLoader<JobRunrExecutor> serviceLoader = ServiceLoader.load(JobRunrExecutor.class);
        return stream(spliteratorUnknownSize(serviceLoader.iterator(), Spliterator.ORDERED), false)
                .sorted((a, b) -> compare(b.getPriority(), a.getPriority()))
                .findFirst()
                .orElse(new ScheduledThreadPoolJobRunrExecutor(workDistributionStrategy.getWorkerCount(), "backgroundjob-worker-pool"));
    }
}
