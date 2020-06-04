package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.jobrunr.scheduling.JobId;
import org.jobrunr.storage.listeners.JobChangeListener;
import org.jobrunr.storage.listeners.JobStatsChangeListener;
import org.jobrunr.storage.listeners.JobStorageChangeListener;
import org.jobrunr.utils.resilience.RateLimiter;
import org.jobrunr.utils.streams.StreamUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static java.time.Instant.now;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private final Set<JobStorageChangeListener> onChangeListeners = ConcurrentHashMap.newKeySet();
    private final RateLimiter changeListenerNotificationRateLimit;
    private ReentrantLock reentrantLock;
    private Timer timer;

    public AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
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
    public void addJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.add(listener);
        if (timer == null) {
            try {
                if (reentrantLock.tryLock()) {
                    timer = new Timer(true);
                    timer.schedule(new SendJobStatsUpdate(), 5000, 5000);
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public void removeJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.remove(listener);
        if (onChangeListeners.isEmpty()) {
            try {
                if (reentrantLock.tryLock()) {
                    timer.cancel();
                    timer = null;
                }
            } finally {
                reentrantLock.unlock();
            }
        }
    }

    @Override
    public void close() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    protected void notifyOnChangeListenersIf(boolean mustNotify) {
        if (mustNotify) {
            notifyOnChangeListeners();
        }
    }

    protected void notifyOnChangeListeners() {
        try {
            if (onChangeListeners.isEmpty()) return;
            if (changeListenerNotificationRateLimit.isRateLimited()) return;

            final List<JobStatsChangeListener> jobStatsChangeListeners = StreamUtils
                    .ofType(onChangeListeners, JobStatsChangeListener.class)
                    .collect(toList());
            if (!jobStatsChangeListeners.isEmpty()) {
                JobStats jobStats = getJobStats();
                jobStatsChangeListeners.forEach(listener -> listener.onChange(jobStats));
            }
        } catch (Exception e) {
            // TODO what to do
        }
    }

    private void notifyJobChangeListeners() {
        try {
            final Map<JobId, List<JobChangeListener>> listenerByJob = StreamUtils
                    .ofType(onChangeListeners, JobChangeListener.class)
                    .collect(groupingBy(JobChangeListener::getJobId));
            System.out.println("Found " + listenerByJob.size() + " listeners");
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
            // TODO what to do
        }
    }

    class SendJobStatsUpdate extends TimerTask {

        public void run() {
            System.out.println(now().toString() + " - Send update from TimerTask");
            notifyOnChangeListeners();
            notifyJobChangeListeners();
        }
    }
}
