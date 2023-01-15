package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.jmx.BackgroundJobServerMBean;
import org.jobrunr.server.jmx.JobServerStats;
import org.jobrunr.server.runner.*;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.tasks.CheckForNewJobRunrVersion;
import org.jobrunr.server.tasks.CheckIfAllJobsExistTask;
import org.jobrunr.server.tasks.CreateClusterIdIfNotExists;
import org.jobrunr.server.tasks.MigrateFromV5toV6Task;
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
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Integer.compare;
import static java.util.Arrays.asList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.JobUtils.assertJobExists;

public class BackgroundJobServer implements BackgroundJobServerMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final UUID backgroundJobServerId;
    private final BackgroundJobServerConfiguration configuration;
    private final StorageProvider storageProvider;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final JsonMapper jsonMapper;
    private final List<BackgroundJobRunner> backgroundJobRunners;
    private final JobDefaultFilters jobDefaultFilters;
    private final JobServerStats jobServerStats;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final ServerZooKeeper serverZooKeeper;
    private final JobZooKeeper jobZooKeeper;
    private final BackgroundJobServerLifecycleLock lifecycleLock;
    private final BackgroundJobPerformerFactory backgroundJobPerformerFactory;
    private volatile Instant firstHeartbeat;
    private volatile boolean isRunning;
    private volatile Boolean isMaster;
    private volatile ScheduledThreadPoolExecutor zookeeperThreadPool;
    private JobRunrExecutor jobExecutor;

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, null);
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator) {
        this(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration configuration) {
        if (storageProvider == null)
            throw new IllegalArgumentException("A StorageProvider is required to use a BackgroundJobServer. Please see the documentation on how to setup a job StorageProvider.");

        this.backgroundJobServerId = UUID.randomUUID();
        this.configuration = configuration;
        this.storageProvider = new ThreadSafeStorageProvider(storageProvider);
        this.dashboardNotificationManager = new DashboardNotificationManager(backgroundJobServerId, storageProvider);
        this.jsonMapper = jsonMapper;
        this.backgroundJobRunners = initializeBackgroundJobRunners(jobActivator);
        this.jobDefaultFilters = new JobDefaultFilters();
        this.jobServerStats = new JobServerStats();
        this.workDistributionStrategy = createWorkDistributionStrategy(configuration);
        this.serverZooKeeper = createServerZooKeeper();
        this.jobZooKeeper = createJobZooKeeper();
        this.backgroundJobPerformerFactory = loadBackgroundJobPerformerFactory();
        this.lifecycleLock = new BackgroundJobServerLifecycleLock();
    }

    public UUID getId() {
        return backgroundJobServerId;
    }

    public void start() {
        start(true);
    }

    public void start(boolean guard) {
        if (guard) {
            if (isStarted()) return;
            try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
                firstHeartbeat = Instant.now();
                isRunning = true;
                startZooKeepers();
                startWorkers();
            }
        }
    }

    public void pauseProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before pausing");
        if (isPaused()) return;
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            isRunning = false;
            stopWorkers();
            LOGGER.info("Paused job processing");
        }
    }

    public void resumeProcessing() {
        if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before resuming");
        if (isProcessing()) return;
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            startWorkers();
            isRunning = true;
            LOGGER.info("Resumed job processing");
        }
    }

    public void stop() {
        if (isStopped()) return;
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            LOGGER.info("BackgroundJobServer and BackgroundJobPerformers - stopping (waiting for all jobs to complete - max 10 seconds)");
            isMaster = null;
            stopWorkers();
            stopZooKeepers();
            isRunning = false;
            firstHeartbeat = null;
            LOGGER.info("BackgroundJobServer and BackgroundJobPerformers stopped");
        }
    }

    public boolean isAnnounced() {
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            return isMaster != null;
        }
    }

    public boolean isUnAnnounced() {
        return !isAnnounced();
    }

    public boolean isMaster() {
        return isAnnounced() && isMaster;
    }

    void setIsMaster(Boolean isMaster) {
        if (isStopped()) return;

        this.isMaster = isMaster;
        if (isMaster != null) {
            LOGGER.info("JobRunr BackgroundJobServer ({}) using {} and {} BackgroundJobPerformers started successfully", getId(), storageProvider.getStorageProviderInfo().getName(), workDistributionStrategy.getWorkerCount());
            if(isMaster) {
                runStartupTasks();
            }
        } else {
            LOGGER.error("JobRunr BackgroundJobServer failed to start");
        }
    }

    public boolean isRunning() {
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            return isRunning;
        }
    }

    public BackgroundJobServerStatus getServerStatus() {
        return new BackgroundJobServerStatus(
                backgroundJobServerId, configuration.getName(), workDistributionStrategy.getWorkerCount(),
                configuration.getPollIntervalInSeconds(), configuration.getDeleteSucceededJobsAfter(), configuration.getPermanentlyDeleteDeletedJobsAfter(),
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

    public BackgroundJobServerConfiguration getConfiguration() {
        return configuration;
    }

    public DashboardNotificationManager getDashboardNotificationManager() {
        return dashboardNotificationManager;
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

    public JobDefaultFilters getJobFilters() {
        return jobDefaultFilters;
    }

    BackgroundJobRunner getBackgroundJobRunner(Job job) {
        assertJobExists(job.getJobDetails());
        return backgroundJobRunners.stream()
                .filter(jobRunner -> jobRunner.supports(job))
                .findFirst()
                .orElseThrow(() -> problematicConfigurationException("Could not find a BackgroundJobRunner: either no JobActivator is registered, your Background Job Class is not registered within the IoC container or your Job does not have a default no-arg constructor."));
    }

    public void processJob(Job job) {
        BackgroundJobPerformer backgroundJobPerformer = backgroundJobPerformerFactory.newBackgroundJobPerformer(this, job);
        jobExecutor.execute(backgroundJobPerformer);
        LOGGER.debug("Submitted BackgroundJobPerformer for job {} to executor service", job.getId());
    }

    boolean isStarted() {
        return !isStopped();
    }

    boolean isStopped() {
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            return zookeeperThreadPool == null;
        }
    }

    boolean isPaused() {
        return !isProcessing();
    }

    boolean isProcessing() {
        try (BackgroundJobServerLifecycleLock ignored = lifecycleLock.lock()) {
            return isRunning;
        }
    }

    private void startZooKeepers() {
        zookeeperThreadPool = new ScheduledThreadPoolJobRunrExecutor(2, "backgroundjob-zookeeper-pool");
        // why fixedDelay: in case of long stop-the-world garbage collections, the zookeeper tasks will queue up
        // and all will be launched one after another
        zookeeperThreadPool.scheduleWithFixedDelay(serverZooKeeper, 0, configuration.getPollIntervalInSeconds(), TimeUnit.SECONDS);
        zookeeperThreadPool.scheduleWithFixedDelay(jobZooKeeper, 1, configuration.getPollIntervalInSeconds(), TimeUnit.SECONDS);
        zookeeperThreadPool.scheduleWithFixedDelay(new CheckForNewJobRunrVersion(this), 1, 8, TimeUnit.HOURS);
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
            List<Runnable> startupTasks = asList(
                    new CreateClusterIdIfNotExists(this),
                    new CheckIfAllJobsExistTask(this),
                    new CheckForNewJobRunrVersion(this),
                    new MigrateFromV5toV6Task(this));
            startupTasks.forEach(jobExecutor::execute);
        } catch (Exception notImportant) {
            // server is shut down immediately
        }
    }

    private List<BackgroundJobRunner> initializeBackgroundJobRunners(JobActivator jobActivator) {
        return asList(
                new BackgroundJobWithIocRunner(jobActivator),
                new BackgroundJobWithoutIocRunner(),
                new BackgroundStaticJobWithoutIocRunner(),
                new BackgroundStaticFieldJobWithoutIocRunner()
        );
    }

    private void stop(ScheduledExecutorService executorService) {
        if (executorService == null) return;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                LOGGER.info("JobRunr BackgroundJobServer shutdown requested - waiting for jobs to finish (at most 10 seconds)");
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
        return configuration.getBackgroundJobServerWorkerPolicy().toWorkDistributionStrategy(this);
    }

    private BackgroundJobPerformerFactory loadBackgroundJobPerformerFactory() {
        ServiceLoader<BackgroundJobPerformerFactory> serviceLoader = ServiceLoader.load(BackgroundJobPerformerFactory.class);
        return stream(spliteratorUnknownSize(serviceLoader.iterator(), Spliterator.ORDERED), false)
                .min((a, b) -> compare(b.getPriority(), a.getPriority()))
                .orElseGet(BasicBackgroundJobPerformerFactory::new);
    }

    private JobRunrExecutor loadJobRunrExecutor() {
        ServiceLoader<JobRunrExecutor> serviceLoader = ServiceLoader.load(JobRunrExecutor.class);
        return stream(spliteratorUnknownSize(serviceLoader.iterator(), Spliterator.ORDERED), false)
                .min((a, b) -> compare(b.getPriority(), a.getPriority()))
                .orElseGet(() -> new ScheduledThreadPoolJobRunrExecutor(workDistributionStrategy.getWorkerCount(), "backgroundjob-worker-pool"));
    }

    private static class BackgroundJobServerLifecycleLock implements AutoCloseable {
        private final ReentrantLock reentrantLock = new ReentrantLock();

        public BackgroundJobServerLifecycleLock lock() {
            if (reentrantLock.isHeldByCurrentThread()) return this;

            reentrantLock.lock();
            return this;
        }

        @Override
        public void close() {
            reentrantLock.unlock();
        }
    }

    private static class BasicBackgroundJobPerformerFactory implements BackgroundJobPerformerFactory {
        @Override
        public int getPriority() {
            return 10;
        }

        @Override
        public BackgroundJobPerformer newBackgroundJobPerformer(BackgroundJobServer backgroundJobServer, Job job) {
            return new BackgroundJobPerformer(backgroundJobServer, job);
        }
    }
}
