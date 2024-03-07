package org.jobrunr.server;

import org.jobrunr.server.tasks.zookeeper.AbstractJobZooKeeperTask;
import org.jobrunr.server.tasks.zookeeper.DeleteSucceededJobsTask;
import org.jobrunr.server.tasks.zookeeper.ProcessScheduledJobsTask;

/**
 * A JobZooKeeper manages is responsible for 1 or more JobZooKeeper Tasks like {@link ProcessScheduledJobsTask} and {@link DeleteSucceededJobsTask}.
 */
public class JobZooKeeper extends JobHandler {

    public JobZooKeeper(BackgroundJobServer backgroundJobServer, AbstractJobZooKeeperTask... zooKeeperTasks) {
        super(backgroundJobServer, zooKeeperTasks);
    }

}