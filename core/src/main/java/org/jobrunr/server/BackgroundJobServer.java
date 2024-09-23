package org.jobrunr.server;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.server.BackgroundJobServer.BackgroundJobServerLifecycleLock.LifeCycleLock;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolver;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.jmx.BackgroundJobServerMBean;
import org.jobrunr.server.jmx.JobServerStats;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticFieldJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.tasks.startup.CheckIfAllJobsExistTask;
import org.jobrunr.server.tasks.startup.CreateClusterIdIfNotExists;
import org.jobrunr.server.tasks.startup.MigrateFromV5toV6Task;
import org.jobrunr.server.tasks.zookeeper.DeleteDeletedJobsPermanentlyTask;
import org.jobrunr.server.tasks.zookeeper.DeleteSucceededJobsTask;
import org.jobrunr.server.tasks.zookeeper.ProcessOrphanedJobsTask;
import org.jobrunr.server.tasks.zookeeper.ProcessRecurringJobsTask;
import org.jobrunr.server.tasks.zookeeper.ProcessScheduledJobsTask;
import org.jobrunr.server.threadpool.JobRunrExecutor;
import org.jobrunr.server.threadpool.PlatformThreadPoolJobRunrExecutor;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.ThreadSafeStorageProvider;
import org.jobrunr.utils.VersionNumber;
import org.jobrunr.utils.mapper.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.lang.Integer.compare;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.jobrunr.JobRunrException.problematicConfigurationException;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.utils.JobUtils.assertJobExists;
import static org.jobrunr.utils.VersionNumber.v;

public class BackgroundJobServer implements BackgroundJobServerMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundJobServer.class);

    private final BackgroundJobServerConfigurationReader configuration;
    private final StorageProvider storageProvider;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final JsonMapper jsonMapper;
    private final List<BackgroundJobRunner> backgroundJobRunners;
    private final JobDefaultFilters jobDefaultFilters;
    private final JobServerStats jobServerStats;
    private final WorkDistributionStrategy workDistributionStrategy;
    private final JobSteward jobSteward;
    private final ServerZooKeeper serverZooKeeper;
    private final ConcurrentJobModificationResolver concurrentJobModificationResolver;
    private final BackgroundJobServerLifecycleLock lifecycleLock;
    private final BackgroundJobPerformerFactory backgroundJobPerformerFactory;
    private volatile Instant firstHeartbeat;
    private volatile boolean isRunning;
    private volatile Boolean isMaster;
    private volatile VersionNumber dataVersion;
    private volatile PlatformThreadPoolJobRunrExecutor zookeeperThreadPool;
    private JobRunrExecutor jobExecutor;

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper) {
        this(storageProvider, jsonMapper, null);
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator) {
        this(storageProvider, jsonMapper, jobActivator, usingStandardBackgroundJobServerConfiguration());
    }

    public BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator, BackgroundJobServerConfiguration configuration) {
        this(storageProvider, jsonMapper, jobActivator, new BackgroundJobServerConfigurationReader(configuration));
    }

    protected BackgroundJobServer(StorageProvider storageProvider, JsonMapper jsonMapper, JobActivator jobActivator, BackgroundJobServerConfigurationReader configuration) {
        if (storageProvider == null)
            throw new IllegalArgumentException("A StorageProvider is required to use a BackgroundJobServer. Please see the documentation on how to setup a job StorageProvider.");

        this.configuration = configuration;
        this.storageProvider = new ThreadSafeStorageProvider(storageProvider);
        this.dashboardNotificationManager = new DashboardNotificationManager(this.configuration.getId(), storageProvider);
        this.jsonMapper = jsonMapper;
        this.backgroundJobRunners = initializeBackgroundJobRunners(jobActivator);
        this.jobDefaultFilters = new JobDefaultFilters();
        this.jobServerStats = new JobServerStats();
        this.workDistributionStrategy = createWorkDistributionStrategy();
        this.jobSteward = createJobSteward();
        this.serverZooKeeper = createServerZooKeeper();
        this.concurrentJobModificationResolver = createConcurrentJobModificationResolver();
        this.backgroundJobPerformerFactory = loadBackgroundJobPerformerFactory();
        this.lifecycleLock = new BackgroundJobServerLifecycleLock();
        this.storageProvider.validatePollInterval(this.configuration.getPollInterval());
    }

    @Override
    public UUID getId() {
        return configuration.getId();
    }

    @Override
    public void start() {
        start(true);
    }

    public void start(boolean guard) {
        if (guard) {
            try (LifeCycleLock ignored = lifecycleLock.writeLock()) {
                if (isStarted()) return;
                firstHeartbeat = Instant.now();
                isRunning = true;
                startStewardAndServerZooKeeper();
                startWorkers();
            }
        }
    }

    @Override
    public void pauseProcessing() {
        try (LifeCycleLock ignored = lifecycleLock.writeLock()) {
            if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before pausing");
            if (isPaused()) return;
            isRunning = false;
            stopWorkers();
            LOGGER.info("Paused job processing");
        }
    }

    @Override
    public void resumeProcessing() {
        try (LifeCycleLock ignored = lifecycleLock.writeLock()) {
            if (isStopped()) throw new IllegalStateException("First start the BackgroundJobServer before resuming");
            if (isProcessing()) return;
            startWorkers();
            isRunning = true;
            LOGGER.info("Resumed job processing");
        }
    }

    @Override
    public void stop() {
        try (LifeCycleLock ignored = lifecycleLock.writeLock()) {
            if (isStopped()) return;
            LOGGER.info("BackgroundJobServer - stopping (may take about {})", configuration.getInterruptJobsAwaitDurationOnStopBackgroundJobServer());
            isMaster = null;
            stopWorkers();
            stopZooKeepers();
            isRunning = false;
            firstHeartbeat = null;
            LOGGER.info("BackgroundJobServer and BackgroundJobPerformers stopped");
        }
    }

    boolean isStarted() {
        return !isStopped();
    }

    boolean isStopped() {
        try (LifeCycleLock ignored = lifecycleLock.readLock()) {
            return zookeeperThreadPool == null;
        }
    }

    boolean isPaused() {
        return !isProcessing();
    }

    boolean isProcessing() {
        try (LifeCycleLock ignored = lifecycleLock.readLock()) {
            return isRunning;
        }
    }

    public boolean isAnnounced() {
        try (LifeCycleLock ignored = lifecycleLock.readLock()) {
            if (isStopping()) return false;
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
        if (isStopping() || isStopped()) return;

        this.isMaster = isMaster;
        if (isMaster != null) {
            LOGGER.info("JobRunr BackgroundJobServer ({}) using {} and {} BackgroundJobPerformers started successfully", getId(), storageProvider.getStorageProviderInfo().getName(), workDistributionStrategy.getWorkerCount());
            if (isMaster) {
                startJobZooKeepers();
                runStartupTasks();
            }
        } else {
            LOGGER.error("JobRunr BackgroundJobServer failed to start");
        }
    }

    @Override
    public boolean isRunning() {
        // why: otherwise all the workers querying this method when they onboard work can cause deadlock
        if (lifecycleLock.isWriteLockInUse()) return false;
        try (LifeCycleLock ignored = lifecycleLock.readLock()) {
            if (isStopping()) return false;
            return isRunning;
        }
    }

    public boolean isNotReadyToProcessJobs() {
        return !(isAnnounced() && hasDataVersion(v("6.0.0")));
    }

    @Override
    public BackgroundJobServerStatus getServerStatus() {
        return new BackgroundJobServerStatus(configuration.getId(), configuration.getName(), workDistributionStrategy.getWorkerCount(),
                (int) configuration.getPollInterval().getSeconds(), configuration.getDeleteSucceededJobsAfter(), configuration.getPermanentlyDeleteDeletedJobsAfter(),
                firstHeartbeat, Instant.now(), isRunning, jobServerStats);
    }

    public JobSteward getJobSteward() {
        return jobSteward;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public ConcurrentJobModificationResolver getConcurrentJobModificationResolver() {
        return concurrentJobModificationResolver;
    }

    public BackgroundJobServerConfigurationReader getConfiguration() {
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

    private void startStewardAndServerZooKeeper() {
        zookeeperThreadPool = new PlatformThreadPoolJobRunrExecutor(5, 5, "backgroundjob-zookeeper-pool");
        // why fixedDelay: in case of long stop-the-world garbage collections, the zookeeper tasks will queue up
        // and all will be launched one after another
        zookeeperThreadPool.scheduleWithFixedDelay(serverZooKeeper, 0, configuration.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
        zookeeperThreadPool.scheduleWithFixedDelay(jobSteward, min(configuration.getPollInterval().toMillis() / 5, 1000), configuration.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void startJobZooKeepers() {
        long delay = min(configuration.getPollInterval().toMillis() / 5, 1000);
        JobZooKeeper recurringAndScheduledJobsZooKeeper = new JobZooKeeper(this, new ProcessRecurringJobsTask(this), new ProcessScheduledJobsTask(this));
        JobZooKeeper orphanedJobsZooKeeper = new JobZooKeeper(this, new ProcessOrphanedJobsTask(this));
        JobZooKeeper janitorZooKeeper = new JobZooKeeper(this, new DeleteSucceededJobsTask(this), new DeleteDeletedJobsPermanentlyTask(this));
        zookeeperThreadPool.scheduleWithFixedDelay(recurringAndScheduledJobsZooKeeper, delay, configuration.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
        zookeeperThreadPool.scheduleWithFixedDelay(orphanedJobsZooKeeper, delay, configuration.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
        zookeeperThreadPool.scheduleWithFixedDelay(janitorZooKeeper, delay, configuration.getPollInterval().toMillis(), TimeUnit.MILLISECONDS);
    }

    private void stopZooKeepers() {
        serverZooKeeper.stop();
        zookeeperThreadPool.stop(Duration.ofSeconds(10));
        this.zookeeperThreadPool = null;
    }

    private void startWorkers() {
        jobExecutor = configuration.getBackgroundJobServerWorkerPolicy().toJobRunrExecutor();
        jobExecutor.start();
    }

    private void stopWorkers() {
        if (jobExecutor == null) return;
        LOGGER.info("BackgroundJobPerformers - stopping (waiting at most {} for jobs to finish)", configuration.getInterruptJobsAwaitDurationOnStopBackgroundJobServer());
        jobExecutor.stop(configuration.getInterruptJobsAwaitDurationOnStopBackgroundJobServer());
        this.jobExecutor = null;
    }

    private void runStartupTasks() {
        try {
            List<Runnable> startupTasks = asList(
                    new CreateClusterIdIfNotExists(this),
                    new CheckIfAllJobsExistTask(this),
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

    protected ServerZooKeeper createServerZooKeeper() {
        return new ServerZooKeeper(this);
    }

    protected JobSteward createJobSteward() {
        return new JobSteward(this);
    }

    protected ConcurrentJobModificationResolver createConcurrentJobModificationResolver() {
        return getConfiguration()
                .getConcurrentJobModificationPolicy()
                .toConcurrentJobModificationResolver(this);
    }

    private boolean hasDataVersion(VersionNumber expectedVersion) {
        if (expectedVersion.equals(dataVersion)) return true;
        JobRunrMetadata metadata = storageProvider.getMetadata("database_version", "cluster");
        if (metadata != null) {
            dataVersion = v(metadata.getValue());
            return expectedVersion.equals(dataVersion);
        }
        return false;
    }

    protected WorkDistributionStrategy createWorkDistributionStrategy() {
        return configuration.getBackgroundJobServerWorkerPolicy().toWorkDistributionStrategy(this);
    }

    private BackgroundJobPerformerFactory loadBackgroundJobPerformerFactory() {
        ServiceLoader<BackgroundJobPerformerFactory> serviceLoader = ServiceLoader.load(BackgroundJobPerformerFactory.class);
        return stream(spliteratorUnknownSize(serviceLoader.iterator(), Spliterator.ORDERED), false)
                .min((a, b) -> compare(b.getPriority(), a.getPriority()))
                .orElseGet(BasicBackgroundJobPerformerFactory::new);
    }

    private boolean isStopping() {
        final JobRunrExecutor tmpJobExecutor = jobExecutor;
        return tmpJobExecutor != null && tmpJobExecutor.isStopping();
    }

    static class BackgroundJobServerLifecycleLock {
        interface LifeCycleLock extends AutoCloseable {
            void close();
        }

        private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        private final LifeCycleLock readClose = () -> reentrantReadWriteLock.readLock().unlock();
        private final LifeCycleLock writeClose = () -> reentrantReadWriteLock.writeLock().unlock();

        public LifeCycleLock readLock() {
            reentrantReadWriteLock.readLock().lock();
            return readClose;
        }

        public LifeCycleLock writeLock() {
            if (reentrantReadWriteLock.getReadHoldCount() > 0)
                throw new IllegalMonitorStateException("Cannot upgrade read to write lock");
            reentrantReadWriteLock.writeLock().lock();
            return writeClose;
        }

        public boolean isWriteLockInUse() {
            return reentrantReadWriteLock.isWriteLocked();
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
