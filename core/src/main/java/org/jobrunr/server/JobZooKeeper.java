package org.jobrunr.server;

import org.jobrunr.server.tasks.zookeeper.JobZooKeeperTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobZooKeeper extends JobHandler {

    static final Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    public JobZooKeeper(BackgroundJobServer backgroundJobServer, JobZooKeeperTask... zooKeeperTasks) {
        super(backgroundJobServer, zooKeeperTasks);
    }

    @Override
    protected Logger logger() {
        return LOGGER;
    }
}