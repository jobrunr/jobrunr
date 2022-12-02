package org.jobrunr.server.dashboard;

import org.jobrunr.server.dashboard.mappers.*;
import org.jobrunr.storage.StorageProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.jobrunr.utils.reflection.ReflectionUtils.newInstance;

public class DashboardNotificationManager {

    private final StorageProvider storageProvider;
    private final Set<DashboardNotificationMapper<?>> notificationMappers;

    public DashboardNotificationManager(UUID backgroundJobServerId, StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.notificationMappers = new HashSet<>(asList(
                new SevereJobRunrExceptionNotificationMapper(backgroundJobServerId, storageProvider),
                new CpuAllocationIrregularityNotificationMapper(backgroundJobServerId),
                new PollIntervalInSecondsTimeBoxIsTooSmallNotificationMapper(backgroundJobServerId),
                new NewJobRunrVersionNotificationMapper()
        ));
    }

    public void handle(Exception e) {
        if (e instanceof DashboardNotification) {
            notify((DashboardNotification) e);
        }
    }

    public void notify(DashboardNotification e) {
        notificationMappers.stream()
                .filter(notificationMapper -> notificationMapper.supports(e))
                .map(notificationMapper -> notificationMapper.map(e))
                .forEach(storageProvider::saveMetadata);
    }

    public void deleteNotification(Class<? extends DashboardNotification> notificationToDelete) {
        storageProvider.deleteMetadata(notificationToDelete.getSimpleName());
    }

    public <T extends DashboardNotification> T getDashboardNotification(Class<T> notificationClass) {
        return storageProvider
                .getMetadata(notificationClass.getSimpleName())
                .stream()
                .map(metadata -> newInstance(notificationClass, metadata))
                .findFirst()
                .orElse(null);
    }
}
