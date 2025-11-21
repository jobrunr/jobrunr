package org.jobrunr.server.dashboard;

import org.jobrunr.server.dashboard.mappers.CarbonIntensityApiErrorNotificationMapper;
import org.jobrunr.server.dashboard.mappers.CpuAllocationIrregularityNotificationMapper;
import org.jobrunr.server.dashboard.mappers.DashboardNotificationMapper;
import org.jobrunr.server.dashboard.mappers.PollIntervalInSecondsTimeBoxIsTooSmallNotificationMapper;
import org.jobrunr.server.dashboard.mappers.SevereJobRunrExceptionNotificationMapper;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.reflection.ReflectionUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;

public class DashboardNotificationManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardNotificationManager.class);

    private final StorageProvider storageProvider;
    private final Set<DashboardNotificationMapper<?>> notificationMappers;

    public DashboardNotificationManager(UUID backgroundJobServerId, StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
        this.notificationMappers = new HashSet<>(asList(
                new SevereJobRunrExceptionNotificationMapper(backgroundJobServerId, storageProvider),
                new CpuAllocationIrregularityNotificationMapper(backgroundJobServerId),
                new PollIntervalInSecondsTimeBoxIsTooSmallNotificationMapper(backgroundJobServerId),
                new CarbonIntensityApiErrorNotificationMapper(backgroundJobServerId)
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
                .forEach(this::saveDashboardNotificationAsMetadata);
    }

    public void deleteNotification(Class<? extends DashboardNotification> notificationToDelete) {
        storageProvider.deleteMetadata(notificationToDelete.getSimpleName());
    }

    public <T extends DashboardNotification> @Nullable T getDashboardNotification(Class<T> notificationClass) {
        return storageProvider
                .getMetadata(notificationClass.getSimpleName())
                .stream()
                .map(metadata -> ReflectionUtils.newInstance(notificationClass, metadata))
                .findFirst()
                .orElse(null);
    }

    private void saveDashboardNotificationAsMetadata(JobRunrMetadata metadata) {
        try {
            storageProvider.saveMetadata(metadata);
        } catch (Exception e) {
            LOGGER.debug("Unable to save dashboard notification metadata", e); // this is acceptable and means the same notification was saved concurrently
        }
    }
}
