package org.jobrunr.scheduling;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestService.Work;
import org.jobrunr.stubs.TestServiceForIoC;
import org.jobrunr.stubs.TestServiceInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.ONE_SECOND;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.scheduling.JobBuilder.aJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;

public class BackgroundJobByIoCJobLambdaTest {

    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private TestServiceForIoC testServiceForIoC;
    private TestServiceInterface testServiceInterface;

    private static final String everySecond = "*/1 * * * * *";

    @BeforeEach
    public void setUpTests() {
        storageProvider = new InMemoryStorageProvider();
        testServiceForIoC = new TestServiceForIoC("a constructor arg");
        testServiceInterface = testServiceForIoC;
        SimpleJobActivator jobActivator = new SimpleJobActivator(testServiceForIoC, new TestService());
        JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(200)))
                .initialize();
        backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    @AfterEach
    public void cleanUp() {
        MDC.clear();
        backgroundJobServer.stop();
        storageProvider.close();
    }

    @Test
    void testCreateViaBuilder() {
        UUID jobId = UUID.randomUUID();
        BackgroundJob.create(aJob()
                .withId(jobId)
                .withName("My Job Name")
                .withAmountOfRetries(3)
                .<TestService>withDetails(x -> x.doWorkAndReturnResult("some string")));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId))
                .hasJobName("My Job Name")
                .hasAmountOfRetries(3)
                .hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueue() {
        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWork());
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasState(SUCCEEDED));
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithMethodReference() {
        JobId jobId = BackgroundJob.<TestService>enqueue(TestService::doWork);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueUsingServiceInstance() {
        JobId jobId = BackgroundJob.enqueue(() -> testServiceForIoC.doWork());
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueUsingServiceInterfaceInstance() {
        JobId jobId = BackgroundJob.enqueue(() -> testServiceInterface.doWork());
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithCustomObject() {
        final TestService.Work work = new TestService.Work(2, "some string", UUID.randomUUID());
        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWork(work));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithPath() {
        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWorkWithPath(Path.of("/tmp/jobrunr/example.log")));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithJobContextAndMetadata() {
        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWork(5, JobContext.Null));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == PROCESSING);
        await().atMost(TEN_SECONDS).until(() -> !storageProvider.getJobById(jobId).getMetadata().isEmpty());
        assertThat(storageProvider.getJobById(jobId))
                .hasMetadata("test", "test");
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId))
                .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
                .hasMetadataOnlyContainingJobProgressAndLogging();
    }

    @Test
    void testEnqueueStreamWithMultipleParameters() {
        Stream<UUID> workStream = getWorkStream();
        AtomicInteger atomicInteger = new AtomicInteger();
        BackgroundJob.<TestService, UUID>enqueue(workStream, (x, uuid) -> x.doWork(uuid.toString(), atomicInteger.incrementAndGet(), now()));

        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testEnqueueStreamWithWrappingObjectAsParameter() {
        AtomicInteger atomicInteger = new AtomicInteger();
        Stream<Work> workStream = getWorkStream()
                .map(uuid -> new Work(atomicInteger.incrementAndGet(), "some string " + uuid, uuid));

        BackgroundJob.<TestService, Work>enqueue(workStream, (x, work) -> x.doWork(work));
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testEnqueueStreamWithParameterFromWrappingObject() {
        AtomicInteger atomicInteger = new AtomicInteger();
        Stream<TestService.Work> workStream = getWorkStream()
                .map(uuid -> new TestService.Work(atomicInteger.incrementAndGet(), "some string " + uuid, uuid));

        BackgroundJob.<TestService, Work>enqueue(workStream, (x, work) -> x.doWork(work.getUuid()));
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testFailedJobAddsFailedStateAndScheduledThanksToDefaultRetryFilter() {
        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWorkThatFails());
        await().atMost(FIVE_SECONDS)
                .untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED));

    }

    @Test
    void testScheduleWithZonedDateTime() {
        JobId jobId = BackgroundJob.<TestService>schedule(ZonedDateTime.now().plus(ofMillis(1500)), x -> x.doWork());
        await().during(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithOffsetDateTime() {
        JobId jobId = BackgroundJob.<TestService>schedule(OffsetDateTime.now().plus(ofMillis(1500)), x -> x.doWork());
        await().during(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithLocalDateTime() {
        JobId jobId = BackgroundJob.<TestService>schedule(LocalDateTime.now().plus(ofMillis(1500)), x -> x.doWork());
        await().during(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithInstant() {
        JobId jobId = BackgroundJob.<TestService>schedule(now().plusMillis(1500), x -> x.doWork());
        await().during(ONE_SECOND).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleUsingDateTimeInTheFutureIsNotEnqueued() {
        JobId jobId = BackgroundJob.<TestService>schedule(now().plus(100, ChronoUnit.DAYS), x -> x.doWork());
        await().during(TWO_SECONDS).atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED);
    }

    @Test
    void testScheduleThatSchedulesOtherJobs() {
        JobId jobId = BackgroundJob.<TestService>schedule(now().plusSeconds(1), x -> x.scheduleNewWork(5));
        await().atMost(ONE_MINUTE).until(() -> storageProvider.countJobs(SUCCEEDED) == (5 + 1));
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJob() {
        BackgroundJob.<TestService>scheduleRecurrently(everySecond, x -> x.doWork(5));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithJobContext() {
        BackgroundJob.<TestService>scheduleRecurrently(everySecond, x -> x.doWork(5, JobContext.Null));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithId() {
        BackgroundJob.<TestService>scheduleRecurrently("theId", everySecond, x -> x.doWork(5));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithIdAndTimezone() {
        BackgroundJob.<TestService>scheduleRecurrently("theId", everySecond, systemDefault(), x -> x.doWork(5));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJob() {
        BackgroundJob.<TestService>scheduleRecurrently(Duration.ofSeconds(1), x -> x.doWork(5));
        await().atMost(ofSeconds(15)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJobWithId() {
        BackgroundJob.<TestService>scheduleRecurrently("theId", Duration.ofSeconds(1), x -> x.doWork(5));
        await().atMost(ofSeconds(15)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobList(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testDeleteOfRecurringJob() {
        String jobId = BackgroundJob.<TestService>scheduleRecurrently(Cron.minutely(), x -> x.doWork(5));
        BackgroundJob.deleteRecurringJob(jobId);
        assertThat(storageProvider.getRecurringJobs()).isEmpty();
    }

    @Test
    void recurringJobIdIsKeptEvenIfBackgroundJobServerRestarts() {
        BackgroundJob.<TestService>scheduleRecurrently("my-job-id", everySecond, x -> x.doWorkThatTakesLong(12));
        await().atMost(TWO_SECONDS).until(() -> storageProvider.countJobs(PROCESSING) == 1);
        final UUID jobId = storageProvider.getJobList(PROCESSING, ascOnUpdatedAt(1000)).get(0).getId();
        backgroundJobServer.stop();

        backgroundJobServer.start();
        await().atMost(ofSeconds(20)).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
        assertThat(storageProvider.getJobById(jobId))
                .hasRecurringJobId("my-job-id")
                .hasStates(SCHEDULED, ENQUEUED, PROCESSING, FAILED, SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void mdcContextIsAvailableInJob() {
        MDC.put("someKey", "someValue");

        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWorkWithMDC("someKey"));
        await().atMost(30, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }

    @Test
    void mdcContextIsAvailableForDisplayName() {
        MDC.put("customer.id", "1");

        JobId jobId = BackgroundJob.<TestService>enqueue(x -> x.doWorkWithAnnotation(5, "John Doe"));
        assertThat(storageProvider.getJobById(jobId)).hasJobName("Doing some hard work for user John Doe (customerId: 1)");
    }

    private Stream<UUID> getWorkStream() {
        return IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID());
    }
}
