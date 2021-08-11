package org.jobrunr.server.dashboard;

import org.jobrunr.server.dashboard.mappers.CpuAllocationIrregularityNotificationMapper;
import org.jobrunr.server.dashboard.mappers.DashboardNotificationMapper;
import org.jobrunr.server.dashboard.mappers.SevereJobRunrExceptionNotificationMapper;
import org.jobrunr.storage.StorageProvider;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;

public class DashboardNotificationManager {

    private final StorageProvider storageProvider;
    private final Set<DashboardNotificationMapper<?>> notificationMappers;

    public DashboardNotificationManager(UUID backgroundJobServerId, StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.notificationMappers = new HashSet<>(asList(
                new SevereJobRunrExceptionNotificationMapper(backgroundJobServerId, storageProvider),
                new CpuAllocationIrregularityNotificationMapper(backgroundJobServerId)
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
}
