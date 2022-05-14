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
        if(!lock.tryLock()) throw new PreviousRunNotFinishedException();
        return new RunTracker(this);
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
            return this.zooKeeperRunManager.runStartTime;
        }

        @Override
        public void close() {
            this.zooKeeperRunManager.runStartTime = null;
            this.zooKeeperRunManager.lock.unlock();
        }

        private void startRun() {
            this.zooKeeperRunManager.runStartTime = Instant.now();
            if(this.zooKeeperRunManager.firstRunStartTime == null) this.zooKeeperRunManager.firstRunStartTime = this.zooKeeperRunManager.runStartTime;
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
