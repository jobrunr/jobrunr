package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.tests.e2e.services.GeoService;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.tests.e2e.services.Work;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.tests.fromhost.HttpClient.getJson;

class E2EJDKTest {

    private TestService testService;

    @BeforeEach
    public void startJobRunr() {
        testService = new TestService();

        JobRunr
                .configure()
                .useStorageProvider(new InMemoryStorageProvider())
                .useJobActivator(this::jobActivator)
                .useDashboard()
                .useBackgroundJobServer()
                .initialize();
    }

    @AfterEach
    public void stopJobRunr() {
        JobRunr
                .destroy();
    }

    @Test
    void usingLambdaWithIoCLookupUsingInstance() {
        BackgroundJob.enqueue(() -> testService.doWork(new Work(1, "Foo", 2L, UUID.randomUUID())));

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    void usingLambdaWithIoCLookupWithoutInstance() {
        BackgroundJob.<TestService>enqueue(x -> x.doWork(new Work(1, "Foo", 2L, UUID.randomUUID())));

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    void usingMethodReference() {
        BackgroundJob.enqueue((JobLambda) testService::doWork);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    void usingMethodReferenceWithoutInstance() {
        BackgroundJob.<TestService>enqueue(TestService::doWork);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    void replicatingJobRunrIssue() {
        ClassLoader loader = E2EJDKTest.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream("org/jobrunr/tests/e2e/E2EJDKTest.class"); DataInputStream data = new DataInputStream(in)) {
            if (0xCAFEBABE != data.readInt()) {
                throw new IOException("invalid header");
            }
            int minor = data.readUnsignedShort();
            int major = data.readUnsignedShort();
            System.out.println(major + "." + minor);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Running GeoService");
        System.out.println("Java version " + System.getProperty("java.version"));
        new GeoService().run();

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    private String getSucceededJobs() {
        return getJson("http://localhost:8000/api/jobs?state=SUCCEEDED");
    }

    private <T> T jobActivator(Class<T> clazz) {
        if(TestService.class.equals(clazz)) {
            return (T) new TestService();
        } else if(GeoService.class.equals(clazz)) {
            return (T) new GeoService();
        }
        throw new UnsupportedOperationException();
    }
}
