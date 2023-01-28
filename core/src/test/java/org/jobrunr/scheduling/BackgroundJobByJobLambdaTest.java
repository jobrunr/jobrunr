package org.jobrunr.scheduling;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.core.read.ListAppender;
import io.github.artsok.RepeatedIfExceptionsTest;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.scheduling.exceptions.JobClassNotFoundException;
import org.jobrunr.scheduling.exceptions.JobMethodNotFoundException;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProviderForTest;
import org.jobrunr.stubs.StaticTestService;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForRecurringJobsIfStopTheWorldGCOccurs;
import org.jobrunr.utils.GCUtils;
import org.jobrunr.utils.SleepUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.classThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.scheduling.JobBuilder.aJob;
import static org.jobrunr.scheduling.RecurringJobBuilder.aRecurringJob;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

/**
 * Must be public as used as a background job
 */
public class BackgroundJobByJobLambdaTest {

    private TestService testService;
    private StorageProviderForTest storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private static final String every5Seconds = "*/5 * * * * *";

    @BeforeEach
    void setUpTests() {
        testService = new TestService();
        testService.reset();
        storageProvider = new StorageProviderForTest(new InMemoryStorageProvider());
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5))
                .initialize();

        backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    @AfterEach
    void cleanUp() {
        MDC.clear();
        backgroundJobServer.stop();
    }

    @Test
    void ifBackgroundJobIsNotConfiguredCorrectlyAnExceptionIsThrown() {
        BackgroundJob.setJobScheduler(null);
        assertThatThrownBy(() -> BackgroundJob.enqueue(() -> System.out.println("Test")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The JobScheduler has not been initialized. Use the fluent JobRunr.configure() API to setup JobRunr or set the JobScheduler via the static setter method.");
    }

    @Test
    void testCreateViaBuilder() {
        UUID jobId = UUID.randomUUID();
        BackgroundJob.create(aJob()
                .withId(jobId)
                .withName("My Job Name")
                .withAmountOfRetries(3)
                .withDetails(() -> testService.doWorkAndReturnResult("some string")));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId))
                .hasJobName("My Job Name")
                .hasAmountOfRetries(3)
                .hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testCreateViaBuilderAndAnnotationMustFail() {
        assertThatThrownBy(() -> BackgroundJob.create(aJob()
                .withDetails(() -> testService.doWorkWithAnnotation(3, "Jef Klak"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("You are combining the JobBuilder with the Job annotation which is not allowed. You can only use one of them.");
    }

    @Test
    void testEnqueueSystemOut() {
        JobId jobId = BackgroundJob.enqueue(() -> System.out.println("this is a test"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueue() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWork());
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithId() {
        UUID id = UUID.randomUUID();
        JobId jobId1 = BackgroundJob.enqueue(id, () -> testService.doWork());
        JobId jobId2 = BackgroundJob.enqueue(id, () -> testService.doWork());
        // why: no exception would be thrown.

        assertThat(jobId1).isEqualTo(jobId2);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId1)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));
    }

    @Test
    public void testEnqueueWithStaticMethod() {
        final JobId jobId = BackgroundJob.enqueue(TestService::doStaticWork);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));
    }

    @Test
    public void testEnqueueWithStaticMethodWithArgument() {
        UUID id = UUID.randomUUID();
        final JobId jobId = BackgroundJob.enqueue(() -> StaticTestService.doWorkInStaticMethod(id));
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));
    }

    @Test
    void testEnqueueWithInterfaceImplementationThrowsNiceException() {
        assertThatThrownBy(() -> BackgroundJob.enqueue(new JobImplementation()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please provide a lambda expression (e.g. BackgroundJob.enqueue(() -> myService.doWork()) instead of an actual implementation.");
    }

    @Test
    void testEnqueueWithCustomObject() {
        final TestService.Work work = new TestService.Work(2, "some string", UUID.randomUUID());
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWork(work));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithPath() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkWithPath(Paths.get("/tmp/jobrunr/example.log")));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithJobContextAndMetadata() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWork(5, JobContext.Null));
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
        BackgroundJob.enqueue(workStream, (uuid) -> testService.doWork(uuid.toString(), atomicInteger.incrementAndGet(), now()));

        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testEnqueueStreamWithWrappingObjectAsParameter() {
        AtomicInteger atomicInteger = new AtomicInteger();
        Stream<TestService.Work> workStream = getWorkStream()
                .map(uuid -> new TestService.Work(atomicInteger.incrementAndGet(), "some string " + uuid, uuid));

        BackgroundJob.enqueue(workStream, (work) -> testService.doWork(work));
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testEnqueueStreamWithParameterFromWrappingObject() {
        AtomicInteger atomicInteger = new AtomicInteger();
        Stream<TestService.Work> workStream = getWorkStream()
                .map(uuid -> new TestService.Work(atomicInteger.incrementAndGet(), "some string " + uuid, uuid));

        BackgroundJob.enqueue(workStream, (work) -> testService.doWork(work.getUuid()));
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testEnqueueStreamWithMethodReference() {
        BackgroundJob.enqueue(getWorkStream(), TestService::doWorkWithUUID);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testCreateStreamWithJobBuilder() {
        BackgroundJob.create(getWorkStream()
                .map(uuid -> aJob().withDetails(() -> System.out.println("this is a test")))
        );

        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void jobsCanEnqueueOtherJobsInTheSameClassUsingMethodReference() {
        JobId jobId = BackgroundJob.enqueue(this::aNestedJob);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));
    }

    @Test
    void testFailedJobAddsFailedStateAndScheduledThanksToDefaultRetryFilter() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatFails());
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED));
    }

    @Test
    void testScheduleWithId() {
        UUID id = UUID.randomUUID();
        JobId jobId1 = BackgroundJob.schedule(id, now(), () -> testService.doWork());
        JobId jobId2 = BackgroundJob.schedule(id, now().plusSeconds(20), () -> testService.doWork());
        // why: no exception whould be thrown.

        assertThat(jobId1).isEqualTo(jobId2);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId1)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED));
    }

    @Test
    void testScheduleWithZonedDateTime() {
        JobId jobId = BackgroundJob.schedule(ZonedDateTime.now().plusSeconds(7), () -> testService.doWork());
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithOffsetDateTime() {
        JobId jobId = BackgroundJob.schedule(OffsetDateTime.now().plusSeconds(7), () -> testService.doWork());
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithLocalDateTime() {
        JobId jobId = BackgroundJob.schedule(LocalDateTime.now().plusSeconds(7), () -> testService.doWork());
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithInstant() {
        JobId jobId = BackgroundJob.schedule(now().plusSeconds(7), () -> testService.doWork());
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleUsingDateTimeInTheFutureIsNotEnqueued() {
        JobId jobId = BackgroundJob.schedule(now().plus(100, ChronoUnit.DAYS), () -> testService.doWork());
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED);
    }

    @Test
    void testScheduleThatSchedulesOtherJobs() {
        JobId jobId = BackgroundJob.schedule(now().plusSeconds(1), () -> testService.scheduleNewWork(5));
        await().atMost(ONE_MINUTE).until(() -> storageProvider.countJobs(SUCCEEDED) == (5 + 1));
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleThatSchedulesOtherJobsSlowlyDoesNotBlockOtherWorkers() {
        JobId jobId = BackgroundJob.schedule(now().plusSeconds(1), () -> testService.scheduleNewWorkSlowly(5));
        await().atMost(ofSeconds(12)).until(() -> (storageProvider.countJobs(PROCESSING) + storageProvider.countJobs(SUCCEEDED)) > 1);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING);
    }

    @Test
    void testRecurringCronJob() {
        BackgroundJob.scheduleRecurrently(every5Seconds, () -> testService.doWork(5));
        await().atMost(65, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 3);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobFromBuilder() {
        BackgroundJob.createRecurrently(aRecurringJob()
                .withCron(every5Seconds)
                .withDetails(() -> testService.doWork(5)));
        await().atMost(65, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 3);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithJobContext() {
        BackgroundJob.scheduleRecurrently(every5Seconds, () -> testService.doWork(5, JobContext.Null));
        await().atMost(65, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithId() {
        BackgroundJob.scheduleRecurrently("theId", every5Seconds, () -> testService.doWork(5));
        await().atMost(25, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithIdAndTimezone() {
        BackgroundJob.scheduleRecurrently("theId", every5Seconds, systemDefault(), () -> testService.doWork(5));
        await().atMost(25, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void testRecurringCronJobDoesNotSkipRecurringJobsIfStopTheWorldGCOccurs() {
        TestServiceForRecurringJobsIfStopTheWorldGCOccurs testService = new TestServiceForRecurringJobsIfStopTheWorldGCOccurs();
        testService.resetProcessedJobs();
        ListAppender logger = LoggerAssert.initFor(testService);
        BackgroundJob.scheduleRecurrently(every5Seconds, testService::doWork);
        await().atMost(65, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        // WHEN
        GCUtils.simulateStopTheWorldGC(20000);

        // THEN
        await().atMost(65, SECONDS)
                .untilAsserted(() -> assertThat(logger).hasInfoMessageContaining("JobRunr recovered from long GC and all jobs were executed"));
        SleepUtils.sleep(20000);
    }

    @Test
    void testRecurringIntervalJob() {
        BackgroundJob.scheduleRecurrently(Duration.ofSeconds(5), () -> testService.doWork(5));
        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJobFromBuilder() {
        BackgroundJob.createRecurrently(aRecurringJob()
                .withDuration(Duration.ofSeconds(5))
                .withDetails(() -> testService.doWork(5)));
        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJobWithId() {
        BackgroundJob.scheduleRecurrently("theId", Duration.ofSeconds(5), () -> testService.doWork(5));
        await().atMost(15, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void test2RecurringJobsWithSameMethodSignatureShouldBothBeRun() {
        BackgroundJob.scheduleRecurrently("recurring-job-1", every5Seconds, systemDefault(), () -> testService.doWork(5));
        BackgroundJob.scheduleRecurrently("recurring-job-2", every5Seconds, systemDefault(), () -> testService.doWork(5));
        await().atMost(25, SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == 2);

        final Job job1 = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        final Job job2 = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(1);
        assertThat(storageProvider.getJobById(job1.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
        assertThat(storageProvider.getJobById(job2.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testDeleteOfRecurringJob() {
        String jobId = BackgroundJob.scheduleRecurrently(Cron.minutely(), () -> testService.doWork(5));
        BackgroundJob.delete(jobId);
        assertThat(storageProvider.getRecurringJobs()).isEmpty();
    }

    @Test
    void jobsStuckInProcessingStateAreRescheduled() {
        Job job = storageProvider.save(anEnqueuedJob().withState(new ProcessingState(backgroundJobServer), now().minus(15, ChronoUnit.MINUTES)).build());
        await().atMost(3, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(job.getId())).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED));
    }

    @Test
    void jobCanBeUpdatedInTheBackgroundAndThenGoToSucceededState() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(10));
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));
        await().atMost(6, SECONDS).untilAsserted(() -> {
            final Job job = storageProvider.getJobById(jobId);
            ProcessingState processingState = job.getJobState();
            assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
            storageProvider.getJobById(jobId).hasState(PROCESSING);
        });
        await().atMost(6, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasState(SUCCEEDED));
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void jobCanBeDeletedWhenEnqueued() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(12));
        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, DELETED);
        });
    }

    @Test
    void jobCanBeDeletedWhenScheduled() {
        JobId jobId = BackgroundJob.schedule(now().plusSeconds(10), () -> testService.doWorkThatTakesLong(12));
        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, DELETED);
        });
    }

    @Test
    void jobCanBeDeletedDuringProcessingState_jobRethrowsInterruptedException() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(12));
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));

        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, DELETED);
        });

        await().during(6, SECONDS).atMost(12, SECONDS).untilAsserted(() -> {
            assertThat(storageProvider.getJobById(jobId)).doesNotHaveState(SUCCEEDED);
        });
    }

    @Test
    void jobCanBeDeletedDuringProcessingState_jobInterruptCurrentThread() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLongInterruptThread(12));
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));

        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, DELETED);
        });

        await().during(12, SECONDS).atMost(18, SECONDS).untilAsserted(() -> {
            assertThat(storageProvider.getJobById(jobId)).doesNotHaveState(SUCCEEDED);
        });
    }

    @Test
    void jobCanBeDeletedDuringProcessingStateIfInterruptible() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatCanBeInterrupted(12));
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));

        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, DELETED);
        });

        await().during(12, SECONDS).atMost(18, SECONDS).untilAsserted(() -> {
            assertThat(storageProvider.getJobById(jobId)).doesNotHaveState(SUCCEEDED);
        });
    }

    @Test
    void jobCanBeDeletedDuringProcessingState_InterruptedExceptionCatched() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLongCatchInterruptException(12));
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));

        BackgroundJob.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, DELETED);
        });

        await().during(12, SECONDS).atMost(18, SECONDS).untilAsserted(() -> {
            assertThat(storageProvider.getJobById(jobId)).doesNotHaveState(SUCCEEDED);
        });
    }

    @Test
    void processingCanBeSkippedUsingElectStateFilters() {
        JobId jobId = BackgroundJob.enqueue(() -> testService.tryToDoWorkButDontBecauseOfSomeBusinessRuleDefinedInTheOnStateElectionFilter());
        await().during(3, SECONDS).atMost(6, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SCHEDULED));

        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, DELETED, SCHEDULED);
    }

    @Test
    void jobToClassThatDoesNotExistGoesToFailedState() {
        Job job = storageProvider.save(anEnqueuedJob().withJobDetails(classThatDoesNotExistJobDetails()).build());
        await().atMost(3, SECONDS).until(() -> storageProvider.getJobById(job.getId()).hasState(FAILED));
        FailedState failedState = storageProvider.getJobById(job.getId()).getJobState();
        assertThat(failedState.getException()).isInstanceOf(JobClassNotFoundException.class);
        Job failedJob = storageProvider.getJobById(job.getId());
        assertThat(failedJob).hasStates(ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void jobToMethodThatDoesNotExistGoesToFailedState() {
        Job job = storageProvider.save(anEnqueuedJob().withJobDetails(methodThatDoesNotExistJobDetails()).build());
        await().atMost(30, SECONDS).until(() -> storageProvider.getJobById(job.getId()).hasState(FAILED));
        FailedState failedState = storageProvider.getJobById(job.getId()).getJobState();
        assertThat(failedState.getException()).isInstanceOf(JobMethodNotFoundException.class);
        await().during(1, SECONDS).until(() -> storageProvider.getJobById(job.getId()).hasState(FAILED));
        Job failedJob = storageProvider.getJobById(job.getId());
        assertThat(failedJob).hasStates(ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void testJobInheritance() {
        SomeSysoutJobClass someSysoutJobClass = new SomeSysoutJobClass(Cron.daily());
        assertThatCode(() -> someSysoutJobClass.schedule()).doesNotThrowAnyException();
    }

    @Test
    void mdcContextIsAvailableInJob() {
        MDC.put("someKey", "someValue");

        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkWithMDC("someKey"));
        await().atMost(30, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }

    @Test
    void mdcContextIsAvailableForDisplayName() {
        MDC.put("customer.id", "1");

        JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkWithAnnotation(5, "John Doe"));
        assertThat(storageProvider.getJobById(jobId)).hasJobName("Doing some hard work for user John Doe (customerId: 1)");
    }

    interface SomeJobInterface {
        void doWork();
    }

    abstract static class SomeJobClass implements SomeJobInterface {

        private final String cron;

        public SomeJobClass(String cron) {
            this.cron = cron;
        }

        public void schedule() {
            BackgroundJob.scheduleRecurrently("test-id", cron, () -> doWork());
        }
    }

    public static class SomeSysoutJobClass extends SomeJobClass {

        public SomeSysoutJobClass(String cron) {
            super(cron);
        }

        @Override
        public void doWork() {
            System.out.println("In doWork method");
        }
    }

    public void aNestedJob() {
        System.out.println("Nothing else to do");
    }

    static class JobImplementation implements JobLambda {

        @Override
        public void run() throws Exception {
            System.out.println("This should not be run");
        }
    }

    private Stream<UUID> getWorkStream() {
        return IntStream.range(0, 5)
                .mapToObj(i -> UUID.randomUUID());
    }
}
