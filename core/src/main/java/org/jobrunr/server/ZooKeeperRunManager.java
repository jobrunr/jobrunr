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
    private volatile Instant actualRunStartTime;

    public ZooKeeperRunManager(BackgroundJobServer backgroundJobServer, Logger logger) {
        this.logger = logger;
        this.pollIntervalInSeconds = backgroundJobServer.getServerStatus().getPollIntervalInSeconds();
        this.lock = new ReentrantLock();
    }

    public RunTracker startRun() {
        if(!lock.tryLock()) throw new PreviousRunNotFinishedException();
        return new RunTracker(this);
    }

    public void stop() {
        try {
            lock.lock();
            this.firstRunStartTime = null;
        } finally {
            lock.unlock();
        }
    }

    public static class PreviousRunNotFinishedException extends RuntimeException {
        public PreviousRunNotFinishedException() {
            super("The Previous run is not finished");
        }
    }

    static class RunTracker implements AutoCloseable {

        private final ZooKeeperRunManager zooKeeperRunManager;

        public RunTracker(ZooKeeperRunManager zooKeeperRunManager) {
            this.zooKeeperRunManager = zooKeeperRunManager;
            this.startRun();
        }

        public Instant getRunStartTime() {
            return zooKeeperRunManager.firstRunStartTime.plus(ofSeconds(zooKeeperRunManager.runCounter * zooKeeperRunManager.pollIntervalInSeconds));
        }

        @Override
        public void close() {
            this.zooKeeperRunManager.actualRunStartTime = null;
            this.zooKeeperRunManager.runCounter++;
            this.zooKeeperRunManager.lock.unlock();
        }

        private void startRun() {
            this.zooKeeperRunManager.actualRunStartTime = Instant.now();
            if(this.zooKeeperRunManager.firstRunStartTime == null) this.zooKeeperRunManager.firstRunStartTime = this.zooKeeperRunManager.actualRunStartTime;
            this.logRunDetails();
        }

        private void logRunDetails() {
            if(zooKeeperRunManager.logger.isDebugEnabled()) {
                Instant idealRunStartTime = getRunStartTime();
                zooKeeperRunManager.logger.debug("ZooKeeper run details: runCounter: {}, firstRunStartTime: {}, runStartTime: {}, idealRunStartTime: {}, drift: {} ms",
                        this.zooKeeperRunManager.runCounter,
                        this.zooKeeperRunManager.firstRunStartTime,
                        this.zooKeeperRunManager.actualRunStartTime,
                        idealRunStartTime,
                        Duration.between(this.zooKeeperRunManager.actualRunStartTime, idealRunStartTime).toMillis());
            }
        }
    }
}
