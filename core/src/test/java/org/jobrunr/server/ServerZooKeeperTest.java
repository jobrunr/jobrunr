package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.mappers.JobMapper;
import org.jobrunr.server.dashboard.CpuAllocationIrregularityNotification;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.GCUtils;
import org.jobrunr.utils.mapper.JsonMapper;
import org.jobrunr.utils.mapper.jackson.JacksonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aFastBackgroundJobServerStatus;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.util.reflection.Whitebox.getInternalState;

@ExtendWith(MockitoExtension.class)
class ServerZooKeeperTest {

    private static Logger LOGGER = LoggerFactory.getLogger(ServerZooKeeperTest.class);
    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    @Captor
    private ArgumentCaptor<JobRunrMetadata> jobRunrMetadataToSaveArgumentCaptor;

    @BeforeEach
    void setUp() {
        storageProvider = Mockito.spy(new InMemoryStorageProvider());
        final JsonMapper jsonMapper = new JacksonJsonMapper();
        storageProvider.setJobMapper(new JobMapper(jsonMapper));
        backgroundJobServer = new BackgroundJobServer(storageProvider, jsonMapper, null, usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5).andWorkerCount(10));
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

        assertThat(backgroundJobServer.isMaster()).isTrue();
    }

    @Test
    void onStartServerAnnouncesItselfAndDoesNotBecomeMasterIfItIsNotTheFirstToBeOnline() {
        final BackgroundJobServerStatus master = anotherServer();
        storageProvider.announceBackgroundJobServer(master);

        backgroundJobServer.start();

        await().untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(2));

        assertThat(backgroundJobServer.isMaster()).isFalse();
    }

    @Test
    void serverKeepsSignalingItsAlive() {
        backgroundJobServer.start();

        sleep(1000);

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers().get(0).getLastHeartbeat()).isCloseTo(Instant.now(), within(500, MILLIS)));
    }

    @Test
    void masterDoesZookeepingAndKeepsHisMasterStatus() {
        backgroundJobServer.start();

        storageProvider.announceBackgroundJobServer(anotherServer());

        await()
                .pollInterval(ONE_SECOND)
                //.conditionEvaluationListener(condition -> System.out.printf("%s (elapsed time %dms, remaining time %dms)\n", condition.getDescription(), condition.getElapsedTimeInMS(), condition.getRemainingTimeInMS()))
                .atLeast(20, TimeUnit.SECONDS)
                .atMost(55, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        assertThat(backgroundJobServer.isMaster()).isTrue();
        verify(storageProvider, atLeastOnce()).removeTimedOutBackgroundJobServers(any());
        verify(storageProvider, atMost(2)).removeTimedOutBackgroundJobServers(any());
    }

    @Test
    void otherServersDoZookeepingAndBecomeMasterIfMasterStops() {
        final BackgroundJobServerStatus master = anotherServer();
        storageProvider.announceBackgroundJobServer(master);

        backgroundJobServer.start();

        await().atMost(TWO_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isFalse());

        storageProvider.signalBackgroundJobServerStopped(master);

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        await().atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isTrue());
    }

    @Test
    void otherServersDoZookeepingAndBecomeMasterIfMasterCrashes() {
        final BackgroundJobServerStatus master = anotherServer();
        storageProvider.announceBackgroundJobServer(master);

        backgroundJobServer.start();

        await().atMost(TWO_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isFalse());

        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atLeast(15, TimeUnit.SECONDS)
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));

        await().atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isTrue());
        verify(storageProvider, times(1)).removeTimedOutBackgroundJobServers(any());
    }

    @Test
    void aServerThatSignalsItsAliveAlthoughItTimedOutRestartsCompletely3TimesAndThenShutsDown() {
        backgroundJobServer.start();
        sleep(100);

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .atMost(6, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).hasSize(1));
        await().untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isTrue());

        storageProvider.removeTimedOutBackgroundJobServers(Instant.now());
        await().pollInterval(ONE_HUNDRED_MILLISECONDS)
                .during(FIVE_SECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getBackgroundJobServers()).isEmpty());
        await().untilAsserted(() -> assertThat(backgroundJobServer.isMaster()).isFalse());
    }

    @Test
    void serverIsShutdownIfServerZooKeeperCrashes() {
        Mockito.doThrow(new IllegalStateException()).when(storageProvider).announceBackgroundJobServer(any());

        backgroundJobServer.start();

        await().untilAsserted(() -> assertThat(backgroundJobServer.isStopped()).isTrue());
        assertThat(Whitebox.<Object>getInternalState(backgroundJobServer, "zookeeperThreadPool")).isNull();
    }

    @Test
    void backgroundJobServerSignalsItIsStoppedWhenItIsStopped() {
        backgroundJobServer.start();
        await().untilAsserted(() -> verify(storageProvider).announceBackgroundJobServer(any()));

        backgroundJobServer.stop();
        await().untilAsserted(() -> verify(storageProvider).signalBackgroundJobServerStopped(any()));
    }

    @Test
    public void testStopByServerZookeeperDoesCorrectLogging() throws InterruptedException {
        // GIVEN
        final Object serverZooKeeper = getInternalState(backgroundJobServer, "serverZooKeeper");
        ListAppender<ILoggingEvent> zookeeperLogger = LoggerAssert.initFor(serverZooKeeper);
        backgroundJobServer.start();
        LOGGER.info("Let JobRunr startup");
        Thread.sleep(2000);

        // WHEN
        GCUtils.simulateStopTheWorldGC(25000);
        LOGGER.info("Let JobRunr recover");
        Thread.sleep(2000);

        // THEN
        await().atMost(1, TimeUnit.SECONDS).untilAsserted(() -> assertThat(zookeeperLogger).hasNoErrorMessageContaining("An unrecoverable error occurred. Shutting server down..."));
        verify(storageProvider, atLeastOnce()).saveMetadata(jobRunrMetadataToSaveArgumentCaptor.capture());
        assertThat(jobRunrMetadataToSaveArgumentCaptor.getValue())
                .hasName(CpuAllocationIrregularityNotification.class.getSimpleName())
                .hasOwner("BackgroundJobServer " + backgroundJobServer.getId().toString());
    }

    private BackgroundJobServerStatus anotherServer() {
        return aFastBackgroundJobServerStatus().withIsStarted().build();
    }
}