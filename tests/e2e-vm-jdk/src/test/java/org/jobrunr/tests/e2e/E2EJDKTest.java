package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.tests.e2e.services.Work;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.jobs.details.JobDetailsGeneratorUtils.toFQResource;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.tests.fromhost.HttpClient.getJson;
import static org.jobrunr.tests.fromhost.HttpClient.ok;
import static org.jobrunr.utils.StringUtils.isNullOrEmpty;

@EnabledIfEnvironmentVariable(named = "JDK_TEST", matches = "true")
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

    @AfterEach
    public void clearStorageProviderExceptBackgroundJobServers() {
        storageProvider.clear();
    }

    @AfterAll
    public static void stopJobRunr() {
        JobRunr
                .destroy();
    }

    @Test
    void dashboardWebServerIsReachable() {
        assertThat(ok("http://localhost:8000/dashboard")).isTrue();
    }

    @Test
    void testExpectedJavaClassMajorVersion() throws IOException {
        String expectedJavaClassVersion = System.getenv("JAVA_CLASS_VERSION");
        if (isNullOrEmpty(expectedJavaClassVersion)) throw new IllegalStateException("The environment variable 'JAVA_CLASS_VERSION' is missing");

        String actualJavaClassVersion = getJavaClassMajorVersion(testService);
        if (isNullOrEmpty(actualJavaClassVersion)) throw new IllegalStateException("The actual Java Class Version may not be null or empty. ");

        assertThat(actualJavaClassVersion).isEqualTo(expectedJavaClassVersion);
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
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    void usingLambdaWithInvokeVirtualOnJava17AndHigher() {
        testService.run();

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
        return clazz.cast(testService);
    }

    private static String getJavaClassMajorVersion(Object object) throws IOException {
        String classLocation = "/" + toFQResource(object.getClass().getName()) + ".class";
        try (InputStream in = object.getClass().getResourceAsStream(classLocation); DataInputStream data = new DataInputStream(in)) {
            if (0xCAFEBABE != data.readInt()) {
                throw new IOException("invalid header");
            }
            int minor = data.readUnsignedShort();
            int major = data.readUnsignedShort();
            return major + "." + minor;
        }
    }
}
