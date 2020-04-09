package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.utils.Stopwatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Durations.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public abstract class AbstractE2ESqlTest {

    private StorageProvider storageProvider;

    protected abstract StorageProvider getStorageProviderForClient();

    protected abstract AbstractBackgroundJobSqlContainer backgroundJobServer();

    @BeforeEach
    public void setUpJobRunr() {
        storageProvider = getStorageProviderForClient();

        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .initialize();
    }

    @Test
    public void testProcessInBackgroundJobServer() {
        TestService testService = new TestService();
        UUID jobId = BackgroundJob.enqueue(() -> testService.doWork());

        with()
                //.conditionEvaluationListener(condition -> System.out.printf("Processing not done. Server logs:\n %s", backgroundJobServer.getLogs()))
                .pollInterval(FIVE_SECONDS)
                .await().atMost(30, TimeUnit.SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);

        System.out.println(backgroundJobServer().getLogs());
    }

    @Disabled
    @Test
    public void performanceTest() {
        TestService testService = new TestService();

        Stream<UUID> workStream = IntStream
                .range(0, 5000)
                .mapToObj((i) -> UUID.randomUUID());

        Stopwatch stopwatch = new Stopwatch();
        try (Stopwatch start = stopwatch.start()) {
            BackgroundJob.enqueue(workStream, uuid -> testService.doWork());
        }
        System.out.println("Time taken to enqueue 5000 jobs: " + stopwatch.duration().getSeconds() + " s");

        try (Stopwatch start = stopwatch.start()) {
            await()
                    .atMost(TEN_SECONDS)
                    .pollInterval(FIVE_HUNDRED_MILLISECONDS)
                    .untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5000));
        }
        System.out.println("Time taken to process 5000 jobs: " + stopwatch.duration().getSeconds() + " s");
    }
}
