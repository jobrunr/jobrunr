package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.utils.Stopwatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJobThatTakesLong;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

@Testcontainers
public class LoadTest {

    @Container
    private PostgreSQLContainer sqlContainer = new PostgreSQLContainer<>();

    private TestService testService;
    private StorageProvider jobStorageProvider;

    @BeforeEach
    public void setUpTests() throws IOException {
        Files.deleteIfExists(Paths.get("/tmp/code.txt"));

        testService = new TestService();
        testService.reset();
        jobStorageProvider = new PostgresStorageProvider(getPostgresDataSource());
        JobRunr.configure()
                .useStorageProvider(jobStorageProvider)
                .initialize();

    }

    @Disabled
    @Test
    public void testLoadTest() throws InterruptedException {
        int amountOfWork = 1000;
        int amountOfServers = 10;

        getWorkStream(amountOfWork)
                .forEach(id -> jobStorageProvider.save(anEnqueuedJobThatTakesLong().build()));

        Stopwatch stopwatch = new Stopwatch();
        try (Stopwatch start = stopwatch.start()) {
            List<BackgroundJobServer> backgroundJobServerList = new ArrayList<>();
            for (int i = 0; i < amountOfServers; i++) {
                final BackgroundJobServer backgroundJobServer = new BackgroundJobServer(jobStorageProvider, null, 15, 10);
                backgroundJobServerList.add(backgroundJobServer);
                backgroundJobServer.start();
                Thread.sleep(1000L);
            }

            await()
                    .atMost(1, TimeUnit.HOURS)
                    .untilAsserted(() -> assertThat(jobStorageProvider.countJobs(ENQUEUED)).isEqualTo(0L));
            await().atMost(ONE_MINUTE).untilAsserted(() -> assertThat(jobStorageProvider.countJobs(SUCCEEDED)).isEqualTo(amountOfWork));
        }
        System.out.println("Time taken to process " + amountOfWork + " jobs with " + amountOfServers + " servers: " + stopwatch.duration().getSeconds() + " s");
    }

    private Stream<UUID> getWorkStream(int amount) {
        return IntStream.range(0, amount)
                .mapToObj(i -> UUID.randomUUID());
    }

    private PGSimpleDataSource getPostgresDataSource() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setURL(sqlContainer.getJdbcUrl());
        dataSource.setUser(sqlContainer.getUsername());
        dataSource.setPassword(sqlContainer.getPassword());
        return dataSource;
    }
}
