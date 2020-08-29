package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobId;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.StorageProviderChangeListener;
import org.jobrunr.utils.resilience.RateLimiter;
import org.jobrunr.utils.streams.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractStorageProvider.class);

    private final Set<StorageProviderChangeListener> onChangeListeners;
    private final JobStatsEnricher jobStatsEnricher;
    private final RateLimiter changeListenerNotificationRateLimit;
    private final ReentrantLock reentrantLock;
    private Timer timer;

    public AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.onChangeListeners = ConcurrentHashMap.newKeySet();
        this.jobStatsEnricher = new JobStatsEnricher();
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
        this.reentrantLock = new ReentrantLock();
    }

    @Override
    public int delete(UUID id) {
        final Job jobToDelete = getJobById(id);
        jobToDelete.delete();
        save(jobToDelete);
        return 1;
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
    }

    protected void notifyOnChangeListenersIf(boolean mustNotify) {
        if (mustNotify) {
            notifyJobStatsOnChangeListeners();
        }
    }

    protected void notifyJobStatsOnChangeListeners() {
        try {
            if (changeListenerNotificationRateLimit.isRateLimited()) return;

            final List<JobStatsChangeListener> jobStatsChangeListeners = StreamUtils
                    .ofType(onChangeListeners, JobStatsChangeListener.class)
                    .collect(toList());
            if (!jobStatsChangeListeners.isEmpty()) {
                JobStatsExtended extendedJobStats = jobStatsEnricher.enrich(getJobStats());
                jobStatsChangeListeners.forEach(listener -> listener.onChange(extendedJobStats));
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
        if (timer == null) {
            try {
                if (reentrantLock.tryLock()) {
                    timer = new Timer(true);
                    timer.schedule(new SendJobStatsUpdate(), 3000, 5000);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    void stopTimerToSendUpdates() {
        if (timer != null) {
            try {
                if (reentrantLock.tryLock()) {
                    if (timer != null) {
                        timer.cancel();
                        timer = null;
                    }
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    private void logError(Exception e) {
        LOGGER.error("Error notifying JobStorageChangeListeners - please create a bug report (with the stacktrace attached)", e);
    }

    class SendJobStatsUpdate extends TimerTask {

        public void run() {
            notifyJobStatsOnChangeListeners();
            notifyJobChangeListeners();
            notifyBackgroundJobServerStatusChangeListeners();
        }
    }
}
