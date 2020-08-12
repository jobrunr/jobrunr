package org.jobrunr.server;

import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

class ServerZooKeeperTest {

    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    void setUp() {
        storageProvider = Mockito.spy(new InMemoryStorageProvider());
        storageProvider.setJobMapper(new JobMapper(new JacksonJsonMapper()));
        backgroundJobServer = new BackgroundJobServer(storageProvider, null, 5, 10);
    }

    @AfterEach
    void tearDown() {
        try {
            backgroundJobServer.stop();
        } catch (Exception e) {
            e.printStackTrace();
            // not that important
        }
    }

    @Test
    void onStartServerAnnouncesItselfAndBecomesMasterIfItIsTheFirstToBeOnline() {
        backgroundJobServer.start();

        await().untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue();
    }

    @Test
    void onStartServerAnnouncesItselfAndDoesNotBecomeMasterIfItIsNotTheFirstToBeOnline() {
        final BackgroundJobServerStatus master = anotherServer();
        storageProvider.announceBackgroundJobServer(master);

        backgroundJobServer.start();

        await().untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(2));

        assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isFalse();
    }

    @Test
    void serverKeepsSignalingItsAlive() throws InterruptedException {
        backgroundJobServer.start();

        Thread.sleep(1000);

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers().get(0).getLastHeartbeat()).isCloseTo(Instant.now(), within(500, MILLIS)));
    }

    @Test
    void masterDoesZookeepingAndKeepsHisMasterStatus() throws InterruptedException {
        backgroundJobServer.start();

        storageProvider.announceBackgroundJobServer(anotherServer());

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atLeast(20, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue();
    }

    @Test
    void otherServersDoZookeepingAndBecomeMasterIfMasterIsGone() throws InterruptedException {
        final BackgroundJobServerStatus master = anotherServer();
        storageProvider.announceBackgroundJobServer(master);

        backgroundJobServer.start();

        await().atMost(TWO_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster())
                        .isNotNull()
                        .isFalse());

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atLeast(15, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        await().atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue());
    }

    @Test
    void aServerThatSignalsItsAliveAlthoughItTimedOutRestartsCompletely3TimesAndThenShutsDown() throws InterruptedException {
        backgroundJobServer.start();
        Thread.sleep(100);

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .during(FIVE_SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(0));
        await().untilAsserted(() -> assertThat(backgroundJobServer.getJobZooKeeper().isMaster()).isFalse());
    }

    @Test
    void serverIsShutdownIfServerZooKeeperCrashes() {
        Mockito.doThrow(new IllegalStateException()).when(storageProvider).announceBackgroundJobServer(any());

        backgroundJobServer.start();

        await().untilAsserted(() -> assertThat(backgroundJobServer.isStopped()).isTrue());
    }

    @Test
    public void backgroundJobServerSignalsItIsStoppedWhenItIsStoppedAndClosesTheStorageProvider() {
        backgroundJobServer.start();
        await().untilAsserted(() -> verify(storageProvider).announceBackgroundJobServer(any()));

        backgroundJobServer.stop();
        await().untilAsserted(() -> verify(storageProvider).signalBackgroundJobServerStopped(any()));
        await().untilAsserted(() -> verify(storageProvider).close());
    }

    private BackgroundJobServerStatus anotherServer() {
        final BackgroundJobServerStatus masterBackgroundJobServerStatus = new ServerZooKeeper.BackgroundJobServerStatusWriteModel(new BackgroundJobServerStatus(5, 10));
        masterBackgroundJobServerStatus.start();
        return masterBackgroundJobServerStatus;
    }

}