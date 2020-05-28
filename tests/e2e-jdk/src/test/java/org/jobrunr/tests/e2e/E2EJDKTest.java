package org.jobrunr.tests.e2e;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.tests.e2e.services.TestService;
import org.jobrunr.utils.mapper.gson.GsonJsonMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.tests.fromhost.HttpClient.getJson;

public class E2EJDKTest {

    private TestService testService;

    @BeforeEach
    public void startJobRunr() {
        testService = new TestService();

        JobRunr
                .configure()
                .useStorageProvider(new SimpleStorageProvider().withJsonMapper(new GsonJsonMapper()))
                .useJobActivator(this::jobActivator)
                .useDashboard()
                .useDefaultBackgroundJobServer()
                .initialize();
    }

    @AfterEach
    public void stopJobRunr() {
        //todo: stop
    }

    @Test
    void usingLambdaWithIoCLookupUsingInstance() {
        BackgroundJob.enqueue(() -> testService.doWork(UUID.randomUUID()));

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    @Disabled
    void usingLambdaWithIoCLookupWithoutInstance() {
        BackgroundJob.<TestService>enqueue(x -> x.doWork(UUID.randomUUID()));

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    @Disabled
    void usingMethodReference() {
        BackgroundJob.enqueue((JobLambda)testService::doWork);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    @Test
    @Disabled
    void usingMethodReferenceWithoutInstance() {
        BackgroundJob.<TestService>enqueue(TestService::doWork);

        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThatJson(getSucceededJobs()).inPath("$.items[0].jobHistory[2].state").asString().contains("SUCCEEDED"));
    }

    private String getSucceededJobs() {
        final String json = getJson("http://localhost:8000/api/jobs/default/succeeded");
        return json;
    }

    private <T> T jobActivator(Class<T> clazz) {
        return (T) testService;
    }
}
