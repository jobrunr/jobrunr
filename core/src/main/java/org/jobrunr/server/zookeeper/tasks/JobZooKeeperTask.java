package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;

public abstract class JobZooKeeperTask extends ZooKeeperTask {

    protected final JobZooKeeper jobZooKeeper;

    protected JobZooKeeperTask(JobZooKeeper jobZooKeeper, BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
        this.jobZooKeeper = jobZooKeeper;
    }
}
