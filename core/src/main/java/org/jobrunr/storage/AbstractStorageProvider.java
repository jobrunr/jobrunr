package org.jobrunr.storage;

import org.jobrunr.utils.resilience.RateLimiter;

import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public abstract class AbstractStorageProvider implements StorageProvider, AutoCloseable {

    private final Set<JobStorageChangeListener> onChangeListeners = new HashSet<>();
    private final RateLimiter changeListenerNotificationRateLimit;
    private Timer timer;

    public AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
    }

    @Override
    public void addJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.add(listener);
        if (timer == null) {
            synchronized (onChangeListeners) {
                timer = new Timer(true);
                timer.schedule(new SendJobStatsUpdate(), 5000, 5000);
            }
        }
    }

    @Override
    public void removeJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.remove(listener);
        if (onChangeListeners.isEmpty()) {
            synchronized (onChangeListeners) {
                timer.cancel();
                timer = null;
            }
        }
    }

    @Override
    public void close() throws Exception {
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
        if (onChangeListeners.isEmpty()) return;
        if (changeListenerNotificationRateLimit.isRateLimited()) return;

        JobStats jobStats = getJobStats();
        onChangeListeners.forEach(listener -> listener.onChange(jobStats));
    }

    class SendJobStatsUpdate extends TimerTask {

        public void run() {
            AbstractStorageProvider.this.notifyOnChangeListeners();
        }
    }
}
