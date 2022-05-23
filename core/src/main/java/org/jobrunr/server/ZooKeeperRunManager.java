package org.jobrunr.server;

import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

import static java.time.Duration.ofSeconds;

public class ZooKeeperRunManager {

    private final Logger logger;
    private final int pollIntervalInSeconds;
    private final ReentrantLock lock;
    private Instant firstRunStartTime;
    private long runCounter;
    private volatile Instant runStartTime;

    public ZooKeeperRunManager(BackgroundJobServer backgroundJobServer, Logger logger) {
        this.logger = logger;
        this.pollIntervalInSeconds = backgroundJobServer.getServerStatus().getPollIntervalInSeconds();
        this.lock = new ReentrantLock();
    }

    public RunTracker startRun() {
        try {
            lock.lock();
            if(runStartTime != null) throw new IllegalStateException("A new run can only start if the previous run is finished.");
            return new RunTracker(this);
        } finally {
            lock.unlock();
        }
    }

    public boolean isPreviousRunNotFinished() {
        try {
            lock.lock();
            boolean previousRunNotFinished = runStartTime != null;
            if(previousRunNotFinished) {
                logger.error("Skipping run as previous run is not finished. This means the pollIntervalInSeconds setting is too small. This can result in an unstable cluster or recurring jobs that are skipped.");
            }
            return previousRunNotFinished;
        } finally {
            lock.unlock();
        }
    }

    static class RunTracker implements AutoCloseable {

        private final ZooKeeperRunManager zooKeeperRunManager;

        public RunTracker(ZooKeeperRunManager zooKeeperRunManager) {
            this.zooKeeperRunManager = zooKeeperRunManager;
            this.startRun();
        }

        public Instant getRunStartTime() {
            return this.zooKeeperRunManager.runStartTime;
        }

        @Override
        public void close() {
            this.zooKeeperRunManager.runStartTime = null;
        }

        private void startRun() {
            this.zooKeeperRunManager.runStartTime = Instant.now();
            if(this.zooKeeperRunManager.firstRunStartTime == null) {
                this.zooKeeperRunManager.firstRunStartTime = this.zooKeeperRunManager.runStartTime;
            }
            this.logRunDetails();
            this.zooKeeperRunManager.runCounter++;
        }

        private void logRunDetails() {
            if(zooKeeperRunManager.logger.isDebugEnabled()) {
                Instant idealRunStartTime = zooKeeperRunManager.firstRunStartTime.plus(ofSeconds(zooKeeperRunManager.runCounter * zooKeeperRunManager.pollIntervalInSeconds));
                zooKeeperRunManager.logger.debug("ZooKeeper run details: runCounter: {}, firstRunStartTime: {}, runStartTime: {}, idealRunStartTime: {}, drift: {} ms",
                        this.zooKeeperRunManager.runCounter,
                        this.zooKeeperRunManager.firstRunStartTime,
                        this.zooKeeperRunManager.runStartTime,
                        idealRunStartTime,
                        Duration.between(this.zooKeeperRunManager.runStartTime, idealRunStartTime).toMillis());
            }
        }
    }
}
