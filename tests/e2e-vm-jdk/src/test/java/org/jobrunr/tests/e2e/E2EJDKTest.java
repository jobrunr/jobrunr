package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.tests.e2e.services.Work;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.tests.fromhost.HttpClient.getJson;
import static org.jobrunr.utils.reflection.ReflectionUtils.getValueFromFieldOrProperty;

class E2EJDKTest {

    private static final TestService testService = new TestService();

    private static final InMemoryStorageProvider storageProvider = new InMemoryStorageProvider();

    @BeforeAll
    public static void startJobRunr() {
        JobRunr
                .configure()
                .useStorageProvider(storageProvider)
                .useJobActivator(E2EJDKTest::jobActivator)
                .useDashboard()
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .initialize();
    }

    @BeforeEach
    public void clearStorageProviderExceptBackgroundJobServers() {
        // we cannot use whitebox as it is compiled with Java > 8 and some tests will fail
        ((Map) getValueFromFieldOrProperty(storageProvider, "jobQueue")).clear();
        ((List) getValueFromFieldOrProperty(storageProvider, "recurringJobs")).clear();
        ((Map) getValueFromFieldOrProperty(storageProvider, "metadata")).clear();
    }

    @AfterAll
    public static void stopJobRunr() {
        JobRunr
                .destroy();
    }

    @Test
    void usingLambdaWithIoCLookupUsingInstance() {
        BackgroundJob.enqueue(() -> testService.doWork(new Work(1, "Foo", 2L, UUID.randomUUID())));

        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String succeededJobs = getSucceededJobs();
                    assertThatJson(succeededJobs).inPath("$.items").isArray().hasSize(1);
                    assertThatJson(succeededJobs).inPath("$.items[0].jobHistory[2].state").isEqualTo("SUCCEEDED");
                });
    }

    @Test
    void usingLambdaWithIoCLookupWithoutInstance() {
        BackgroundJob.<TestService>enqueue(x -> x.doWork(new Work(1, "Foo", 2L, UUID.randomUUID())));

        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String succeededJobs = getSucceededJobs();
                    assertThatJson(succeededJobs).inPath("$.items").isArray().hasSize(1);
                    assertThatJson(succeededJobs).inPath("$.items[0].jobHistory[2].state").isEqualTo("SUCCEEDED");
                });
    }

    @Test
    void usingMethodReference() {
        BackgroundJob.enqueue((JobLambda) testService::doWork);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String succeededJobs = getSucceededJobs();
                    assertThatJson(succeededJobs).inPath("$.items").isArray().hasSize(1);
                    assertThatJson(succeededJobs).inPath("$.items[0].jobHistory[2].state").isEqualTo("SUCCEEDED");
                });
    }

    @Test
    void usingMethodReferenceWithoutInstance() {
        BackgroundJob.<TestService>enqueue(TestService::doWork);

        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String succeededJobs = getSucceededJobs();
                    assertThatJson(succeededJobs).inPath("$.items").isArray().hasSize(1);
                    assertThatJson(succeededJobs).inPath("$.items[0].jobHistory[2].state").isEqualTo("SUCCEEDED");
                });
    }

    @Test
    void usingSchedule() {
        BackgroundJob.<TestService>schedule(now().plusSeconds(5), TestService::doWork);

        await()
                .atMost(20, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String succeededJobs = getSucceededJobs();
                    assertThatJson(succeededJobs).inPath("$.items").isArray().hasSize(1);
                    assertThatJson(succeededJobs).inPath("$.items[0].jobHistory[3].state").isEqualTo("SUCCEEDED");
                });
    }

    private String getSucceededJobs() {
        return getJson("http://localhost:8000/api/jobs?state=SUCCEEDED");
    }

    private static <T> T jobActivator(Class<T> clazz) {
        return (T) testService;
    }
}
