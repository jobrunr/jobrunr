package org.jobrunr.storage;

import org.jobrunr.utils.resilience.RateLimiter;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractStorageProvider implements StorageProvider {

    private final Set<JobStorageChangeListener> onChangeListeners = new HashSet<>();
    private final RateLimiter changeListenerNotificationRateLimit;

    public AbstractStorageProvider(RateLimiter changeListenerNotificationRateLimit) {
        this.changeListenerNotificationRateLimit = changeListenerNotificationRateLimit;
    }

    @Override
    public void addJobStorageOnChangeListener(JobStorageChangeListener listener) {
        onChangeListeners.add(listener);
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
}
