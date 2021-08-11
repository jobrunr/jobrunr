package org.jobrunr.server;

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
    private final StorageProvider storageProvider;
    private final Duration timeoutDuration;
    private final AtomicInteger restartAttempts;
    private UUID masterId;
    private Instant lastSignalAlive;
    private Instant lastServerTimeoutCheck;

    public ServerZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.timeoutDuration = Duration.ofSeconds(backgroundJobServer.getServerStatus().getPollIntervalInSeconds()).multipliedBy(4);
        this.restartAttempts = new AtomicInteger();
        this.lastSignalAlive = Instant.now();
        this.lastServerTimeoutCheck = Instant.now();
    }

    @Override
    public void run() {
        if (backgroundJobServer.isStopped()) return;

        try {
            if (backgroundJobServer.isUnAnnounced()) {
                announceBackgroundJobServer();
            } else {
                signalBackgroundJobServerAliveAndDoZooKeeping();
            }
        } catch (Exception shouldNotHappen) {
            LOGGER.error("An unrecoverable error occurred. Shutting server down...", shouldNotHappen);
            if (masterId == null) backgroundJobServer.setIsMaster(null);
            new Thread(this::stopServer).start();
        }
    }

    public synchronized void stop() {
        try {
            storageProvider.signalBackgroundJobServerStopped(backgroundJobServer.getServerStatus());
            masterId = null;
        } catch (Exception e) {
            LOGGER.error("Error when signalling that BackgroundJobServer stopped", e);
        }
    }

    private void announceBackgroundJobServer() {
        final BackgroundJobServerStatus serverStatus = backgroundJobServer.getServerStatus();
        storageProvider.announceBackgroundJobServer(serverStatus);
        determineIfCurrentBackgroundJobServerIsMaster();
        lastSignalAlive = serverStatus.getLastHeartbeat();
    }

    private void signalBackgroundJobServerAliveAndDoZooKeeping() {
        try {
            signalBackgroundJobServerAlive();
            deleteServersThatTimedOut();
            determineIfCurrentBackgroundJobServerIsMaster();
        } catch (ServerTimedOutException e) {
            if (restartAttempts.getAndIncrement() < 3) {
                LOGGER.error("SEVERE ERROR - Server timed out while it's still alive. Are all servers using NTP and in the same timezone? Are you having long GC cycles? Restart attempt {} out of 3", restartAttempts);
                new Thread(this::resetServer).start();
            } else {
                LOGGER.error("FATAL - Server restarted 3 times but still times out by other servers. Shutting down.");
                new Thread(this::stopServer).start();
            }
        }
    }

    private void signalBackgroundJobServerAlive() {
        // TODO: stop server if requested?
        final BackgroundJobServerStatus serverStatus = backgroundJobServer.getServerStatus();
        final boolean keepRunning = storageProvider.signalBackgroundJobServerAlive(serverStatus);
        lastSignalAlive = serverStatus.getLastHeartbeat();
        // TODO: if big difference between now, previousSignalAlive or lastSignalAlive, there are GC problems. Show message in UI?
        //LOGGER.info("setting lastSignalAlive to {};", lastSignalAlive);
    }

    private void deleteServersThatTimedOut() {
        if (Instant.now().isAfter(this.lastServerTimeoutCheck.plus(timeoutDuration))) {
            final Instant timedOutInstant = lastSignalAlive.minusSeconds(1);
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
            if (masterId.equals(backgroundJobServer.getId())) {
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
}
