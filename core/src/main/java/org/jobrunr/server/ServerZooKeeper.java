package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

import static java.util.Comparator.comparing;

public class ServerZooKeeper implements Runnable {

    private static Logger LOGGER = LoggerFactory.getLogger(JobZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final StorageProvider storageProvider;
    private final Duration timeoutDuration;
    private boolean isAnnounced;
    private int restartAttempts = 0;

    public ServerZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.timeoutDuration = Duration.ofSeconds(backgroundJobServerStatus().getPollIntervalInSeconds()).multipliedBy(4);
    }

    @Override
    public void run() {
        try {
            if (isUnannounced()) {
                announceBackgroundJobServer();
            } else {
                signalBackgroundJobServerAliveAndDoZooKeeping();
            }
        } catch (Exception shouldNotHappen) {
            LOGGER.error("An unrecoverable error occurred. Shutting server down...", shouldNotHappen);
            new Thread(this::stopServer).start();
        }
    }

    private boolean isUnannounced() {
        return !isAnnounced;
    }

    private void announceBackgroundJobServer() {
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus());
        jobZooKeeper().setIsMaster(determineIfBackgroundJobServerIsMaster());
        isAnnounced = true;
    }

    private void signalBackgroundJobServerAliveAndDoZooKeeping() {
        try {
            final boolean keepRunning = storageProvider.signalBackgroundJobServerAlive(backgroundJobServerStatus());
            final Instant timedOutInstant = Instant.now().minus(timeoutDuration);
            final int amountOfServersThatTimedOut = storageProvider.removeTimedOutBackgroundJobServers(timedOutInstant);
            if (amountOfServersThatTimedOut > 0) {
                LOGGER.info("Removed {} server(s) that timed out", amountOfServersThatTimedOut);
                if (!jobZooKeeper().isMaster()) {
                    LOGGER.info("Starting master reelection process");
                    jobZooKeeper().setIsMaster(determineIfBackgroundJobServerIsMaster());
                }
            }
            // TODO: stop server if requested?
        } catch (ServerTimedOutException e) {
            if (restartAttempts++ < 3) {
                LOGGER.error("SEVERE ERROR - Server timed out while it's still alive. Are all servers using NTP and in the same timezone? Restart attempt {} out of 3", restartAttempts);
                new Thread(this::resetServer).start();
            } else {
                LOGGER.error("FATAL - Server restarted 3 times but still times out by other servers. Shutting down.");
                new Thread(this::stopServer).start();
            }
        }
    }

    private boolean determineIfBackgroundJobServerIsMaster() {
        final BackgroundJobServerStatus oldestServer = storageProvider
                .getBackgroundJobServers()
                .stream()
                .sorted(comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                .findFirst()
                .orElseThrow(() -> JobRunrException.shouldNotHappenException("No servers available?!"));
        final boolean isMaster = oldestServer.getId().equals(backgroundJobServerStatus().getId());
        if (isMaster) {
            LOGGER.info("Server {} is master", backgroundJobServerStatus().getId());
        }
        return isMaster;
    }

    private void resetServer() {
        isAnnounced = false;
        jobZooKeeper().setIsMaster(false);
        backgroundJobServer.stop();
        backgroundJobServer.start();
    }

    private void stopServer() {
        jobZooKeeper().setIsMaster(false);
        isAnnounced = false;
        backgroundJobServer.stop();
    }

    private BackgroundJobServerStatus backgroundJobServerStatus() {
        return backgroundJobServer.getServerStatus();
    }

    private JobZooKeeper jobZooKeeper() {
        return backgroundJobServer.getJobZooKeeper();
    }
}
