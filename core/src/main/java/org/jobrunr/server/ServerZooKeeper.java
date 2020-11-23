package org.jobrunr.server;

import org.jobrunr.server.jmx.JobServerStats;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerZooKeeper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final BackgroundJobServerStatusWriteModel backgroundJobServerStatus;
    private final StorageProvider storageProvider;
    private final Duration timeoutDuration;
    private final AtomicInteger restartAttempts;
    private UUID masterId;
    private Instant lastServerTimeoutCheck;

    public ServerZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.backgroundJobServerStatus = getBackgroundJobServerStatusWriteModel(backgroundJobServer);
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.timeoutDuration = Duration.ofSeconds(backgroundJobServerStatus.getPollIntervalInSeconds()).multipliedBy(4);
        this.restartAttempts = new AtomicInteger();
        this.lastServerTimeoutCheck = Instant.now();
    }

    @Override
    public void run() {
        try {
            if (backgroundJobServer.isUnAnnounced()) {
                announceBackgroundJobServer();
            } else {
                signalBackgroundJobServerAliveAndDoZooKeeping();
            }
        } catch (Exception shouldNotHappen) {
            LOGGER.error("An unrecoverable error occurred. Shutting server down...", shouldNotHappen);
            backgroundJobServer.setIsMaster(null);
            new Thread(this::stopServer).start();
        }
    }

    public synchronized void stop() {
        try {
            storageProvider.signalBackgroundJobServerStopped(backgroundJobServerStatus);
            masterId = null;
        } catch (Exception e) {
            LOGGER.error("Error when signalling that BackgroundJobServer stopped", e);
        }
    }

    protected BackgroundJobServerStatusWriteModel getBackgroundJobServerStatusWriteModel(BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerStatusWriteModel(backgroundJobServer.getServerStatus());
    }

    private void announceBackgroundJobServer() {
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus);
        determineIfCurrentBackgroundJobServerIsMaster();
    }

    private void signalBackgroundJobServerAliveAndDoZooKeeping() {
        try {
            final boolean keepRunning = storageProvider.signalBackgroundJobServerAlive(backgroundJobServerStatus);
            deleteServersThatTimedOut();
            determineIfCurrentBackgroundJobServerIsMaster();
            // TODO: stop server if requested?
        } catch (ServerTimedOutException e) {
            if (restartAttempts.getAndIncrement() < 3) {
                LOGGER.error("SEVERE ERROR - Server timed out while it's still alive. Are all servers using NTP and in the same timezone? Restart attempt {} out of 3", restartAttempts);
                new Thread(this::resetServer).start();
            } else {
                LOGGER.error("FATAL - Server restarted 3 times but still times out by other servers. Shutting down.");
                new Thread(this::stopServer).start();
            }
        }
    }

    private void deleteServersThatTimedOut() {
        if (Instant.now().isAfter(this.lastServerTimeoutCheck.plus(timeoutDuration))) {
            final Instant timedOutInstant = Instant.now().minus(timeoutDuration);
            final int amountOfServersThatTimedOut = storageProvider.removeTimedOutBackgroundJobServers(timedOutInstant);
            if (amountOfServersThatTimedOut > 0) {
                LOGGER.info("Removed {} server(s) that timed out", amountOfServersThatTimedOut);
            }
            this.lastServerTimeoutCheck = Instant.now();
        }
    }

    private void determineIfCurrentBackgroundJobServerIsMaster() {
        UUID longestRunningBackgroundJobServerId = storageProvider.getLongestRunningBackgroundJobServerId();
        if (this.masterId == null || !masterId.equals(longestRunningBackgroundJobServerId)) {
            this.masterId = longestRunningBackgroundJobServerId;
            if (masterId.equals(backgroundJobServerStatus.getId())) {
                backgroundJobServer.setIsMaster(true);
                LOGGER.info("Server {} is master (this BackgroundJobServer)", masterId);
            } else {
                backgroundJobServer.setIsMaster(false);
                LOGGER.info("Server {} is master (another BackgroundJobServer)", masterId);
            }
        }
    }

    private void resetServer() {
        backgroundJobServer.stop();
        backgroundJobServer.start();
    }

    private void stopServer() {
        backgroundJobServer.stop();
    }


    public static class BackgroundJobServerStatusWriteModel extends BackgroundJobServerStatus {

        private final JobServerStats jobServerStats;
        private final BackgroundJobServerStatus serverStatusDelegate;

        public BackgroundJobServerStatusWriteModel(BackgroundJobServerStatus serverStatusDelegate) {
            super(serverStatusDelegate.getWorkerPoolSize(), serverStatusDelegate.getPollIntervalInSeconds(), serverStatusDelegate.getDeleteSucceededJobsAfter(), serverStatusDelegate.getPermanentlyDeleteDeletedJobsAfter());
            this.jobServerStats = new JobServerStats();
            this.serverStatusDelegate = serverStatusDelegate;
        }

        @Override
        public UUID getId() {
            return serverStatusDelegate.getId();
        }

        @Override
        public int getWorkerPoolSize() {
            return serverStatusDelegate.getWorkerPoolSize();
        }

        @Override
        public int getPollIntervalInSeconds() {
            return serverStatusDelegate.getPollIntervalInSeconds();
        }

        @Override
        public Duration getDeleteSucceededJobsAfter() {
            return serverStatusDelegate.getDeleteSucceededJobsAfter();
        }

        @Override
        public Duration getPermanentlyDeleteDeletedJobsAfter() {
            return serverStatusDelegate.getPermanentlyDeleteDeletedJobsAfter();
        }

        @Override
        public Instant getFirstHeartbeat() {
            return serverStatusDelegate.getFirstHeartbeat();
        }

        @Override
        public Instant getLastHeartbeat() {
            return Instant.now();
        }

        @Override
        public void start() {
            serverStatusDelegate.start();
        }

        @Override
        public void pause() {
            serverStatusDelegate.pause();
        }

        @Override
        public void resume() {
            serverStatusDelegate.resume();
        }

        @Override
        public void stop() {
            serverStatusDelegate.stop();
        }

        @Override
        public boolean isRunning() {
            return serverStatusDelegate.isRunning();
        }

        @Override
        public Long getSystemTotalMemory() {
            return jobServerStats.getSystemTotalMemory();
        }

        @Override
        public Long getSystemFreeMemory() {
            return jobServerStats.getSystemFreeMemory();
        }

        @Override
        public Double getSystemCpuLoad() {
            return jobServerStats.getSystemCpuLoad();
        }

        @Override
        public Long getProcessMaxMemory() {
            return jobServerStats.getProcessMaxMemory();
        }

        @Override
        public Long getProcessFreeMemory() {
            return jobServerStats.getProcessFreeMemory();
        }

        @Override
        public Long getProcessAllocatedMemory() {
            return jobServerStats.getProcessAllocatedMemory();
        }

        @Override
        public Double getProcessCpuLoad() {
            return jobServerStats.getProcessCpuLoad();
        }
    }
}
