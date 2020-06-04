package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.ServerTimedOutException;
import org.jobrunr.storage.StorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Comparator.comparing;
import static org.jobrunr.utils.reflection.ReflectionUtils.cast;

public class ServerZooKeeper implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerZooKeeper.class);

    private final BackgroundJobServer backgroundJobServer;
    private final BackgroundJobServerStatusWriteModel backgroundJobServerStatus;
    private final StorageProvider storageProvider;
    private final Duration timeoutDuration;
    private boolean isAnnounced;
    private final AtomicInteger restartAttempts;

    public ServerZooKeeper(BackgroundJobServer backgroundJobServer) {
        this.backgroundJobServer = backgroundJobServer;
        this.backgroundJobServerStatus = getBackgroundJobServerStatusWriteModel(backgroundJobServer);
        this.storageProvider = backgroundJobServer.getStorageProvider();
        this.timeoutDuration = Duration.ofSeconds(backgroundJobServerStatus.getPollIntervalInSeconds()).multipliedBy(4);
        this.restartAttempts = new AtomicInteger();
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

    public void stop() {
        isAnnounced = false;
    }

    protected BackgroundJobServerStatusWriteModel getBackgroundJobServerStatusWriteModel(BackgroundJobServer backgroundJobServer) {
        return new BackgroundJobServerStatusWriteModel(backgroundJobServer.getServerStatus());
    }

    private boolean isUnannounced() {
        return !isAnnounced;
    }

    private void announceBackgroundJobServer() {
        storageProvider.announceBackgroundJobServer(backgroundJobServerStatus);
        jobZooKeeper().setIsMaster(determineIfBackgroundJobServerIsMaster());
        isAnnounced = true;
    }

    private void signalBackgroundJobServerAliveAndDoZooKeeping() {
        try {
            final boolean keepRunning = storageProvider.signalBackgroundJobServerAlive(backgroundJobServerStatus);
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
            if (restartAttempts.getAndIncrement() < 3) {
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
                .min(comparing(BackgroundJobServerStatus::getFirstHeartbeat))
                .orElseThrow(() -> JobRunrException.shouldNotHappenException("No servers available?!"));
        final boolean isMaster = oldestServer.getId().equals(backgroundJobServerStatus.getId());
        if (isMaster) {
            LOGGER.info("Server {} is master", backgroundJobServerStatus.getId());
        }
        return isMaster;
    }

    private void resetServer() {
        backgroundJobServer.stop();
        backgroundJobServer.start();
    }

    private void stopServer() {
        backgroundJobServer.stop();
    }

    private JobZooKeeper jobZooKeeper() {
        return backgroundJobServer.getJobZooKeeper();
    }

    public static class BackgroundJobServerStatusWriteModel extends BackgroundJobServerStatus {

        private final BackgroundJobServerStatus serverStatusDelegate;
        private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        private Double cachedSystemCpuLoad;
        private Double cachedProcessCpuLoad;

        public BackgroundJobServerStatusWriteModel(BackgroundJobServerStatus serverStatusDelegate) {
            super(serverStatusDelegate.getPollIntervalInSeconds(), serverStatusDelegate.getWorkerPoolSize());
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
            return getMXBeanValue("TotalPhysicalMemorySize");
        }

        @Override
        public Long getSystemFreeMemory() {
            return getMXBeanValue("FreePhysicalMemorySize");
        }

        @Override
        public Double getSystemCpuLoad() {
            final Double systemCpuLoad = getMXBeanValue("SystemCpuLoad");
            if (Double.isNaN(systemCpuLoad)) {
                if (cachedSystemCpuLoad == null) {
                    return (double) -1;
                }
            } else {
                cachedSystemCpuLoad = systemCpuLoad;
            }
            return cachedSystemCpuLoad;
        }

        @Override
        public Long getProcessMaxMemory() {
            return Runtime.getRuntime().maxMemory();
        }

        @Override
        public Long getProcessFreeMemory() {
            return Runtime.getRuntime().maxMemory() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        }

        @Override
        public Long getProcessAllocatedMemory() {
            return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        }

        @Override
        public Double getProcessCpuLoad() {
            final Double processCpuLoad = getMXBeanValue("ProcessCpuLoad");
            if (Double.isNaN(processCpuLoad)) {
                if (cachedProcessCpuLoad == null) {
                    return (double) -1;
                }
            } else {
                cachedProcessCpuLoad = processCpuLoad;
            }
            return cachedProcessCpuLoad;
        }

        // visible for testing
        // see bug JDK-8193878
        <O> O getMXBeanValue(String name) {
            try {
                final Object attribute = ManagementFactory.getPlatformMBeanServer().getAttribute(operatingSystemMXBean.getObjectName(), name);
                return cast(attribute);
            } catch (JMException ex) {
                return cast(-1);
            }
        }
    }
}
