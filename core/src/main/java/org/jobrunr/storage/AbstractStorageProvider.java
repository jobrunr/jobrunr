package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.MetadataChangeListener;
import org.jobrunr.storage.listeners.StorageProviderChangeListener;
import org.jobrunr.utils.ThreadUtils;
import org.jobrunr.utils.resilience.RateLimiter;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageProvider.class);

    private final Set<StorageProviderChangeListener> onChangeListeners;
    private final JobStatsEnricher jobStatsEnricher;
    private final RateLimiter changeListenerNotificationRateLimit;
    private final ReentrantLock schedulerLock;
    private final ReentrantLock notifyJobStatsChangeListenersReentrantLock;
    private final AtomicBoolean jobStatsNotificationQueued;
    private final ScheduledThreadPoolExecutor scheduler;
    private volatile ScheduledFuture<?> scheduledFuture;

    protected AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.onChangeListeners = ConcurrentHashMap.newKeySet();
        this.jobStatsEnricher = new JobStatsEnricher();
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
        this.schedulerLock = new ReentrantLock();
        this.notifyJobStatsChangeListenersReentrantLock = new ReentrantLock();
        this.jobStatsNotificationQueued = new AtomicBoolean(false);

        this.scheduler = new ScheduledThreadPoolExecutor(
                1,
                ThreadUtils.daemonThreadFactory("storage-notifier"),
                new ThreadPoolExecutor.DiscardPolicy()
        );
        this.scheduler.setRemoveOnCancelPolicy(true);
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
        scheduler.shutdownNow();
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

        if (!jobStatsNotificationQueued.compareAndSet(false, true)) return;

        scheduler.execute(() -> {
            try {
                notifyJobStatsOnChangeListeners();
            } finally {
                jobStatsNotificationQueued.set(false);
            }
        });
    }

    protected void notifyJobStatsOnChangeListeners() {
        final List<JobStatsChangeListener> jobStatsChangeListeners = StreamUtils
                .ofType(onChangeListeners, JobStatsChangeListener.class)
                .collect(toList());
        if (jobStatsChangeListeners.isEmpty()) return;

        if (!notifyJobStatsChangeListenersReentrantLock.tryLock()) return;
        try {
            if (changeListenerNotificationRateLimit.isRateLimited()) return;
            JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats());
            jobStatsChangeListeners.forEach(listener -> listener.onChange(extendedJobStats));
        } catch (Exception e) {
          logError(e);
        } finally {
            if (notifyJobStatsChangeListenersReentrantLock.isHeldByCurrentThread()) {
                notifyJobStatsChangeListenersReentrantLock.unlock();
            }
        }
    }

    protected void notifyMetadataChangeListeners(boolean mustNotify) {
        if (mustNotify) {
            notifyMetadataChangeListeners();
        }
    }

    protected void notifyMetadataChangeListeners() {
        try {
            final Map<String, List<MetadataChangeListener>> metadataChangeListenersByName = StreamUtils
                    .ofType(onChangeListeners, MetadataChangeListener.class)
                    .collect(groupingBy(MetadataChangeListener::listenForChangesOfMetadataName));

            if (!metadataChangeListenersByName.isEmpty()) {
                metadataChangeListenersByName.forEach((metadataName, listeners) -> {
                    List<JobRunrMetadata> jobRunrMetadata = getMetadata(metadataName);
                    listeners.forEach(listener -> listener.onChange(jobRunrMetadata));
                });
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private void notifyJobChangeListeners() {
        try {
            final Map<JobId, List<JobChangeListener>> listenerByJob = StreamUtils
                    .ofType(onChangeListeners, JobChangeListener.class)
                    .collect(groupingBy(JobChangeListener::getJobId));
            if (!listenerByJob.isEmpty()) {
                listenerByJob.forEach((jobId, listeners) -> {
                    try {
                        Job job = getJobById(jobId);
                        listeners.forEach(listener -> listener.onChange(job));
                    } catch (JobNotFoundException jobNotFoundException) {
                        // somebody is listening for a Job that does not exist
                        listeners.forEach(jobChangeListener -> {
                            try {
                                jobChangeListener.close();
                            } catch (Exception e) {
                                // Not relevant?
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    private void notifyBackgroundJobServerStatusChangeListeners() {
        try {
            final List<BackgroundJobServerStatusChangeListener> serverChangeListeners = StreamUtils
                    .ofType(onChangeListeners, BackgroundJobServerStatusChangeListener.class)
                    .collect(toList());
            if (!serverChangeListeners.isEmpty()) {
                List<BackgroundJobServerStatus> servers = getBackgroundJobServers();
                serverChangeListeners.forEach(listener -> listener.onChange(servers));
            }
        } catch (Exception e) {
            logError(e);
        }
    }

    void startSchedulerToSendUpdates() {
        if (scheduledFuture == null && schedulerLock.tryLock()) {
            try {
                if (scheduledFuture == null) {
                    scheduledFuture = scheduler.scheduleWithFixedDelay(new NotifyOnChangeListeners(), 3, 5, TimeUnit.SECONDS);
                }
            } finally {
                schedulerLock.unlock();
            }
        }
    }

    void stopSchedulerToSendUpdates() {
        if (scheduledFuture != null && schedulerLock.tryLock()) {
            try {
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(false);
                    scheduledFuture = null;
                }
            } finally {
                schedulerLock.unlock();
            }
        }
    }

    private void logError(Throwable e) {
        if (scheduler.isShutdown() || scheduler.isTerminated()) return; // is being stopped so not interested in it
        LOGGER.warn("Error notifying JobStorageChangeListeners", e);
    }

    class NotifyOnChangeListeners implements Runnable {

        @Override
        public void run() {
            try {
                notifyJobStatsOnChangeListeners();
                notifyJobChangeListeners();
                notifyBackgroundJobServerStatusChangeListeners();
                notifyMetadataChangeListeners();
            } catch (Throwable e) {
                logError(e);
            }
        }
    }
}
