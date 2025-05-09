package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.server.BackgroundJobServer;

public class NoOpZooKeeperTask extends AbstractJobZooKeeperTask {

    public NoOpZooKeeperTask(BackgroundJobServer backgroundJobServer) {
        super(backgroundJobServer);
    }

    @Override
    protected void runTask() {

    }
}
