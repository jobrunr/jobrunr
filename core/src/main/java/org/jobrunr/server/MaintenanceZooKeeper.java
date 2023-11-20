package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.server.zookeeper.ZooKeeperRunTaskInfo;
import org.jobrunr.server.zookeeper.ZooKeeperStatistics;
import org.jobrunr.server.zookeeper.tasks.DeleteDeletedJobsPermanentlyTask;
import org.jobrunr.server.zookeeper.tasks.DeleteSucceededJobsTask;
import org.jobrunr.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jobrunr.JobRunrException.shouldNotHappenException;


public class MaintenanceZooKeeper implements Runnable {
    static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final ZooKeeperStatistics zooKeeperStatistics;
    private final DeleteSucceededJobsTask deleteSucceededJobsTask;
    private final DeleteDeletedJobsPermanentlyTask deleteDeletedJobsPermanentlyTask;

    public MaintenanceZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.zooKeeperStatistics = new ZooKeeperStatistics(
                "MaintenanceZooKeeper",
                backgroundJobServer.getConfiguration().getMaintenancePollIntervalInSeconds(),
                backgroundJobServer.getDashboardNotificationManager()
        );
        this.deleteSucceededJobsTask = new DeleteSucceededJobsTask(backgroundJobServer);
        this.deleteDeletedJobsPermanentlyTask = new DeleteDeletedJobsPermanentlyTask(backgroundJobServer);
    }

    @Override
    public void run() {
        if (backgroundJobServer.isUnAnnounced()) return;
        try (ZooKeeperRunTaskInfo runInfo = zooKeeperStatistics.startRun()) {
            deleteSucceededJobsTask.run(runInfo);
            deleteDeletedJobsPermanentlyTask.run(runInfo);
        } catch (Exception e) {
            zooKeeperStatistics.handleException(e);
            if (zooKeeperStatistics.hasTooManyExceptions()) {
                if (e instanceof StorageException) {
                    LOGGER.error("FATAL - JobRunr encountered too many storage exceptions. Shutting down. Did you know JobRunr Pro has built-in database fault tolerance? Check out https://www.jobrunr.io/en/documentation/pro/database-fault-tolerance/", e);
                } else {
                    LOGGER.error("FATAL - JobRunr encountered too many processing exceptions. Shutting down.", shouldNotHappenException(e));
                }
                backgroundJobServer.stop();
            } else {
                LOGGER.warn(JobRunrException.SHOULD_NOT_HAPPEN_MESSAGE + " - Processing will continue.", e);
            }
        }
    }
}
