package org.jobrunr.server;

import org.jobrunr.server.ZooKeeperRunManager.RunTracker;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerZooKeeper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final StorageProvider storageProvider;
    private final ZooKeeperRunManager zooKeeperRunManager;
    private final DashboardNotificationManager dashboardNotificationManager;
    private final Duration timeoutDuration;
    private final AtomicInteger restartAttempts;
    private UUID masterId;
    private Instant lastSignalAlive;
    private Instant lastServerTimeoutCheck;

    public ServerZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.zooKeeperRunManager = new ZooKeeperRunManager(backgroundJobServer, LOGGER);
        this.dashboardNotificationManager = backgroundJobServer.getDashboardNotificationManager();
        this.timeoutDuration = Duration.ofSeconds(backgroundJobServer.getServerStatus().getPollIntervalInSeconds()).multipliedBy(4);
        this.restartAttempts = new AtomicInteger();
        this.lastSignalAlive = Instant.now();
        this.lastServerTimeoutCheck = Instant.now();
    }

    @Override
    public void run() {
        if (backgroundJobServer.isStopped()) return;
        if (zooKeeperRunManager.isPreviousRunNotFinished()) return;

        try (RunTracker tracker = zooKeeperRunManager.startRun()) {
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
        final BackgroundJobServerStatus serverStatus = backgroundJobServer.getServerStatus();
        storageProvider.signalBackgroundJobServerAlive(serverStatus);
        cpuAllocationIrregularity(lastSignalAlive, serverStatus.getLastHeartbeat()).ifPresent(amountOfSeconds -> dashboardNotificationManager.notify(new CpuAllocationIrregularityNotification(amountOfSeconds)));
        lastSignalAlive = serverStatus.getLastHeartbeat();
    }

    private void deleteServersThatTimedOut() {
        if (Instant.now().isAfter(this.lastServerTimeoutCheck.plus(timeoutDuration))) {
            final Instant now = Instant.now();
            final Instant defaultTimeoutInstant = now.minus(timeoutDuration);
            final Instant timedOutInstantUsingLastSignalAlive = lastSignalAlive.minusMillis(500);
            final Instant timedOutInstant = min(defaultTimeoutInstant, timedOutInstantUsingLastSignalAlive);

            final int amountOfServersThatTimedOut = storageProvider.removeTimedOutBackgroundJobServers(timedOutInstant);
            if (amountOfServersThatTimedOut > 0) {
                LOGGER.info("Removed {} server(s) that timed out", amountOfServersThatTimedOut);
            }
            this.lastServerTimeoutCheck = now;
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

    private static Instant min(Instant instant1, Instant instant2) {
        Instant[] instants = new Instant[]{instant1, instant2};
        Arrays.sort(instants);
        return instants[0];
    }

    private Optional<Integer> cpuAllocationIrregularity(Instant lastSignalAlive, Instant lastHeartbeat) {
        final Instant now = Instant.now();
        final int amount1OfSec = (int) Math.abs(lastHeartbeat.getEpochSecond() - lastSignalAlive.getEpochSecond());
        final int amount2OfSec = (int) (now.getEpochSecond() - lastSignalAlive.getEpochSecond());
        final int amount3OfSec = (int) (now.getEpochSecond() - lastHeartbeat.getEpochSecond());

        final int max = Math.max(amount1OfSec, Math.max(amount2OfSec, amount3OfSec));
        if (max > backgroundJobServer.getServerStatus().getPollIntervalInSeconds() * 2L) {
            return Optional.of(max);
        }
        return Optional.empty();
    }
}
