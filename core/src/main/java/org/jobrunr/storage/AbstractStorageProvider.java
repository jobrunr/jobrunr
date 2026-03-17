package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.storage.listeners.StorageProviderChangeListener;
import org.jobrunr.utils.resilience.RateLimiter;
import org.jobrunr.utils.streams.StreamUtils;
import org.jobrunr.utils.threadpool.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageProvider.class);

    private final Set<StorageProviderChangeListener> onChangeListeners;
    private final JobStatsEnricher jobStatsEnricher;
    private final RateLimiter changeListenerNotificationRateLimit;
    private final ReentrantLock schedulerLock;
    private final AtomicBoolean jobStatsNotificationPending;
    private volatile ScheduledExecutorService scheduler;

    protected AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.onChangeListeners = ConcurrentHashMap.newKeySet();
        this.jobStatsEnricher = new JobStatsEnricher();
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
        this.schedulerLock = new ReentrantLock();
        this.jobStatsNotificationPending = new AtomicBoolean(false);
    }

    @Override
    public StorageProviderInfo getStorageProviderInfo() {
        return new StorageProviderInfo(this);
    }

    @Override
    public void addJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        onChangeListeners.add(listener);
        startSchedulerToSendUpdates();
    }

    @Override
    public void removeJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        onChangeListeners.remove(listener);
        if (onChangeListeners.isEmpty()) {
            stopSchedulerToSendUpdates();
        }
    }

    @Override
    public void close() {
        stopSchedulerToSendUpdates();
        onChangeListeners.clear();
    }

    @Override
    public final void validatePollInterval(Duration pollInterval) {
        if (this instanceof InMemoryStorageProvider) {
            if (pollInterval.compareTo(Duration.ofMillis(200)) < 0) {
                throw new IllegalArgumentException("The smallest supported pollInterval for the InMemoryStorageProvider is 200ms.");
            }
        } else if (pollInterval.compareTo(Duration.ofSeconds(5)) < 0) {
            throw new IllegalArgumentException("The smallest supported pollInterval is 5 seconds - otherwise it will cause to much load on your SQL/noSQL datastore.");
        }
    }

    @Override
    public final void validateRecurringJobInterval(Duration durationBetweenRecurringJobInstances) {
        if (this instanceof InMemoryStorageProvider) {
            if (durationBetweenRecurringJobInstances.compareTo(Duration.ofSeconds(1)) < 0) {
                throw new IllegalArgumentException("The smallest supported duration between recurring job instances for the InMemoryStorageProvider is 1s.");
            }
        } else if (durationBetweenRecurringJobInstances.compareTo(Duration.ofSeconds(5)) < 0) {
            throw new IllegalArgumentException("The smallest supported duration between recurring job instances is 5 seconds (because of the smallest supported pollInterval).");
        }
    }

    protected void notifyJobStatsOnChangeListenersIf(boolean mustNotify) {
        if (!mustNotify) return;

        notifyJobStatsOnChangeListeners();
    }

    protected void notifyJobStatsOnChangeListeners() {
        runInBackgroundThread(this::notifyJobStatsOnChangeListenersOnCurrentThread, this::jobStatsNotificationNotQueued);
    }

    protected void notifyMetadataChangeListenersIf(boolean mustNotify) {
        if (!mustNotify) return;

        notifyMetadataChangeListeners();
    }

    protected void notifyMetadataChangeListeners() {
        runInBackgroundThread(this::notifyMetadataChangeListenersOnCurrentThread);
    }

    private void startSchedulerToSendUpdates() {
        schedulerLock.lock();
        try {
            if (noSchedulerAvailable()) {
                scheduler = Executors.newScheduledThreadPool(1, new NamedThreadFactory("jobrunr-storage-notifier", true));
                ScheduledFuture<?> ignored = scheduler.scheduleWithFixedDelay(new NotifyOnChangeListeners(), 3, 5, TimeUnit.SECONDS);
            }
        } finally {
            schedulerLock.unlock();
        }
    }

    private void stopSchedulerToSendUpdates() {
        schedulerLock.lock();
        try {
            if (scheduler != null) {
                scheduler.shutdownNow();
            }
        } finally {
            jobStatsNotificationPending.set(false);
            schedulerLock.unlock();
        }
    }

    private void runInBackgroundThread(Runnable runnable) {
        runInBackgroundThread(runnable, () -> true);
    }

    private void runInBackgroundThread(Runnable runnable, Supplier<Boolean> shouldNotify) {
        if (noListenersRegistered()) return; // why: no listeners
        try {
            if (schedulerAvailable() && shouldNotify.get()) {
                scheduler.execute(runnable);
            }
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Notification task was rejected", e);
        }
    }

    private void logError(Throwable e) {
        if (noSchedulerAvailable()) return; // is being stopped so not interested in it
        LOGGER.warn("Error notifying JobStorageChangeListeners", e);
    }

    private class NotifyOnChangeListeners implements Runnable {

        @Override
        public void run() {
            try {
                if (jobStatsNotificationNotQueued()) {
                    notifyJobStatsOnChangeListenersOnCurrentThread();
                }
                notifyJobChangeListenersOnCurrentThread();
                notifyBackgroundJobServerStatusChangeListenersOnCurrentThread();
                notifyMetadataChangeListenersOnCurrentThread();
            } catch (Throwable e) {
                logError(e);
            }
        }
    }

    private void notifyJobStatsOnChangeListenersOnCurrentThread() {
        jobStatsNotificationPending.set(false);

        if (changeListenerNotificationRateLimit.isRateLimited()) return;

        final List<JobStatsChangeListener> jobStatsChangeListeners = StreamUtils
                .ofType(onChangeListeners, JobStatsChangeListener.class)
                .collect(toList());

        if (jobStatsChangeListeners.isEmpty()) return;
        try {
            JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats());
            jobStatsChangeListeners.forEach(listener -> listener.onChange(extendedJobStats));
        } catch (Exception e) {
            logError(e);
        }
    }

    private void notifyJobChangeListenersOnCurrentThread() {
        try {
            final Map<JobId, List<JobChangeListener>> listenersByJob = StreamUtils
                    .ofType(onChangeListeners, JobChangeListener.class)
                    .collect(groupingBy(JobChangeListener::getJobId));

            if (listenersByJob.isEmpty()) return;
            listenersByJob.forEach((jobId, listeners) -> {
                try {
                    Job job = getJobById(jobId);
                    listeners.forEach(listener -> listener.onChange(job));
                } catch (JobNotFoundException jobNotFoundException) {
                    // somebody is listening for a Job that does not exist
                    listeners.forEach(this::closeListener);
                }
            });
        } catch (Exception e) {
            logError(e);
        }
    }

    private void notifyBackgroundJobServerStatusChangeListenersOnCurrentThread() {
        try {
            final List<BackgroundJobServerStatusChangeListener> serverChangeListeners = StreamUtils
                    .ofType(onChangeListeners, BackgroundJobServerStatusChangeListener.class)
                    .collect(toList());

            if (serverChangeListeners.isEmpty()) return;
            List<BackgroundJobServerStatus> servers = getBackgroundJobServers();
            serverChangeListeners.forEach(listener -> listener.onChange(servers));
        } catch (Exception e) {
            logError(e);
        }
    }

    protected void notifyMetadataChangeListenersOnCurrentThread() {
        try {
            final Map<String, List<MetadataChangeListener>> metadataChangeListenersByName = StreamUtils
                    .ofType(onChangeListeners, MetadataChangeListener.class)
                    .collect(groupingBy(MetadataChangeListener::listenForChangesOfMetadataName));

            if (metadataChangeListenersByName.isEmpty()) return;
            metadataChangeListenersByName.forEach((metadataName, listeners) -> {
                List<JobRunrMetadata> jobRunrMetadata = getMetadata(metadataName);
                listeners.forEach(listener -> listener.onChange(jobRunrMetadata));
            });
        } catch (Exception e) {
            logError(e);
        }
    }

    private boolean schedulerAvailable() {
        return !noSchedulerAvailable();
    }

    private boolean noSchedulerAvailable() {
        return scheduler == null || scheduler.isShutdown();
    }

    private boolean noListenersRegistered() {
        return scheduler == null;
    }

    private boolean jobStatsNotificationNotQueued() {
        return jobStatsNotificationPending.compareAndSet(false, true);
    }

    private void closeListener(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            // Not relevant
        }
    }
}
