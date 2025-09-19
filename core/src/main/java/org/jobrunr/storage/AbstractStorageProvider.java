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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageProvider.class);

    private final Set<StorageProviderChangeListener> onChangeListeners;
    private final JobStatsEnricher jobStatsEnricher;
    private final RateLimiter changeListenerNotificationRateLimit;
    private final ReentrantLock timerReentrantLock;
    private final ReentrantLock notifyJobStatsChangeListenersReentrantLock;
    private volatile Timer timer;

    protected AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.onChangeListeners = ConcurrentHashMap.newKeySet();
        this.jobStatsEnricher = new JobStatsEnricher();
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
        this.timerReentrantLock = new ReentrantLock();
        this.notifyJobStatsChangeListenersReentrantLock = new ReentrantLock();
    }

    @Override
    public StorageProviderInfo getStorageProviderInfo() {
        return new StorageProviderInfo(this);
    }

    @Override
    public void addJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        onChangeListeners.add(listener);
        startTimerToSendUpdates();
    }

    @Override
    public void removeJobStorageOnChangeListener(StorageProviderChangeListener listener) {
        onChangeListeners.remove(listener);
        if (onChangeListeners.isEmpty()) {
            stopTimerToSendUpdates();
        }
    }

    @Override
    public void close() {
        stopTimerToSendUpdates();
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
        if (mustNotify) {
            notifyJobStatsOnChangeListeners();
        }
    }

    protected void notifyJobStatsOnChangeListeners() {
        final List<JobStatsChangeListener> jobStatsChangeListeners = StreamUtils
                .ofType(onChangeListeners, JobStatsChangeListener.class)
                .collect(toList());
        if (!jobStatsChangeListeners.isEmpty()) {
            CompletableFuture<?> future = runAsync(() -> {
                try {
                    if (!notifyJobStatsChangeListenersReentrantLock.tryLock()) return;
                    if (changeListenerNotificationRateLimit.isRateLimited()) return;
                    JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats());
                    jobStatsChangeListeners.forEach(listener -> listener.onChange(extendedJobStats));
                } finally {
                    if (notifyJobStatsChangeListenersReentrantLock.isHeldByCurrentThread()) {
                        notifyJobStatsChangeListenersReentrantLock.unlock();
                    }
                }
            });
            future.whenComplete((result, e) -> {
                if (e != null) {
                    logError(e);
                }
            });
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

    void startTimerToSendUpdates() {
        if (timer == null && timerReentrantLock.tryLock()) {
            timer = new Timer(true);
            timer.schedule(new NotifyOnChangeListeners(), 3000, 5000);
            timerReentrantLock.unlock();
        }
    }

    void stopTimerToSendUpdates() {
        if (timer != null && timerReentrantLock.tryLock()) {
            timer.cancel();
            timer = null;
            timerReentrantLock.unlock();
        }
    }

    private void logError(Throwable e) {
        if (timerReentrantLock.isLocked() || timer == null) return; // timer is being stopped so not interested in it
        LOGGER.warn("Error notifying JobStorageChangeListeners", e);
    }

    class NotifyOnChangeListeners extends TimerTask {

        @Override
        public void run() {
            notifyJobStatsOnChangeListeners();
            notifyJobChangeListeners();
            notifyBackgroundJobServerStatusChangeListeners();
            notifyMetadataChangeListeners();
        }
    }
}
