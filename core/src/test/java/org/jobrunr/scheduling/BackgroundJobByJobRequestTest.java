package org.jobrunr.scheduling;

import io.github.artsok.RepeatedIfExceptionsTest;
import org.assertj.core.api.Condition;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.cron.Cron;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageProviderForTest;
import org.jobrunr.stubs.TestJobContextJobRequest;
import org.jobrunr.stubs.TestJobContextJobRequest.TestJobContextJobRequestHandler;
import org.jobrunr.stubs.TestJobRequest;
import org.jobrunr.stubs.TestJobRequest.TestJobRequestHandler;
import org.jobrunr.stubs.TestJobRequestThatTakesLong;
import org.jobrunr.stubs.TestMDCJobRequest;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class BackgroundJobByJobRequestTest {

    private StorageProviderForTest storageProvider;
    private BackgroundJobServer backgroundJobServer;

    private static final String every5Seconds = "*/5 * * * * *";

    @BeforeEach
    public void setUpTests() {
        storageProvider = new StorageProviderForTest(new InMemoryStorageProvider());
        SimpleJobActivator jobActivator = new SimpleJobActivator(
                new TestJobRequestHandler(),
                new TestJobContextJobRequestHandler()
        );
        JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5))
                .initialize();
        backgroundJobServer = JobRunr.getBackgroundJobServer();
    }

    @AfterEach
    public void cleanUp() {
        MDC.clear();
        JobRunr.destroy();
    }

    @Test
    void ifBackgroundJobIsNotConfiguredCorrectlyAnExceptionIsThrown() {
        BackgroundJobRequest.setJobRequestScheduler(null);
        assertThatThrownBy(() -> BackgroundJobRequest.enqueue(new TestJobRequest("not important")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("The JobRequestScheduler has not been initialized. Use the fluent JobRunr.configure() API to setup JobRunr or set the JobRequestScheduler via the static setter method.");
    }

    @Test
    void testEnqueue() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueue"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithId() {
        JobId jobId = BackgroundJobRequest.enqueue(UUID.randomUUID(), new TestJobRequest("from testEnqueue"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testEnqueueWithDisplayName() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueue"));
        assertThat(storageProvider.getJobById(jobId))
                .hasJobName("Some neat Job Display Name");
    }

    @Test
    void testEnqueueOfFailingJobAndRetryCount() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueue", true));
        await().atMost(15, TimeUnit.SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == FAILED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED, ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void testEnqueueWithJobContextAndMetadata() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueueWithJobContextAndMetadata", 6));
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
        Stream<JobRequest> workStream = getWorkStream();
        BackgroundJobRequest.enqueue(workStream);

        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.countJobs(SUCCEEDED)).isEqualTo(5));
    }

    @Test
    void testScheduleWithZonedDateTime() {
        JobId jobId = BackgroundJobRequest.schedule(ZonedDateTime.now().plusSeconds(7), new TestJobRequest("from testScheduleWithZonedDateTime"));
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithOffsetDateTime() {
        JobId jobId = BackgroundJobRequest.schedule(OffsetDateTime.now().plusSeconds(7), new TestJobRequest("from testScheduleWithOffsetDateTime"));
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithLocalDateTime() {
        JobId jobId = BackgroundJobRequest.schedule(OffsetDateTime.now().plusSeconds(7), new TestJobRequest("from testScheduleWithLocalDateTime"));
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleWithInstant() {
        JobId jobId = BackgroundJobRequest.schedule(OffsetDateTime.now().plusSeconds(7), new TestJobRequest("from testScheduleWithInstant"));
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testScheduleUsingDateTimeInTheFutureIsNotEnqueued() {
        JobId jobId = BackgroundJobRequest.schedule(now().plus(100, ChronoUnit.DAYS), new TestJobRequest("from testScheduleUsingDateTimeInTheFutureIsNotEnqueued"));
        await().during(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SCHEDULED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(SCHEDULED);
    }

    @Test
    void testRecurringCronJob() {
        BackgroundJobRequest.scheduleRecurrently(every5Seconds, new TestJobRequest("from testRecurringJob"));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithId() {
        BackgroundJobRequest.scheduleRecurrently("theId", every5Seconds, new TestJobRequest("from testRecurringJobWithId"));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringCronJobWithIdAndTimezone() {
        BackgroundJobRequest.scheduleRecurrently("theId", every5Seconds, systemDefault(), new TestJobRequest("from testRecurringJobWithIdAndTimezone"));
        await().atMost(ofSeconds(25)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJob() {
        BackgroundJobRequest.scheduleRecurrently(Duration.ofSeconds(5), new TestJobRequest("from testRecurringJob"));
        await().atMost(ofSeconds(15)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringIntervalJobWithId() {
        BackgroundJobRequest.scheduleRecurrently("theId", Duration.ofSeconds(5), new TestJobRequest("from testRecurringJobWithId"));
        await().atMost(ofSeconds(15)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testDeleteOfRecurringJob() {
        BackgroundJobRequest.scheduleRecurrently("theId", Cron.minutely(), systemDefault(), new TestJobRequest("from testRecurringJobWithIdAndTimezone"));
        BackgroundJob.delete("theId");
        assertThat(storageProvider.getRecurringJobs()).isEmpty();
    }

    @Test
    void recurringJobIdIsKeptEvenIfBackgroundJobServerRestarts() {
        BackgroundJobRequest.scheduleRecurrently("my-job-id", every5Seconds, new TestJobRequestThatTakesLong("from recurringJobIdIsKeptEvenIfBackgroundJobServerRestarts", 20));
        await().atMost(ofSeconds(6)).until(() -> storageProvider.countJobs(PROCESSING) == 1);
        final UUID jobId = storageProvider.getJobs(PROCESSING, ascOnUpdatedAt(1000)).get(0).getId();
        backgroundJobServer.stop();

        backgroundJobServer.start();
        await().atMost(ofSeconds(25)).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
        assertThat(storageProvider.getJobById(jobId))
                .hasRecurringJobId("my-job-id")
                .hasStates(SCHEDULED, ENQUEUED, PROCESSING, FAILED, SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @RepeatedIfExceptionsTest(repeats = 3)
    void jobCanBeDeletedWhenEnqueued() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("input"));
        BackgroundJobRequest.delete(jobId);

        await().atMost(6, SECONDS).untilAsserted(() -> {
            assertThat(backgroundJobServer.getJobZooKeeper().getOccupiedWorkerCount()).isZero();
            assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, DELETED);
        });
    }

    @Test
    void mdcContextIsAvailableInJob() {
        MDC.put("someKey", "someValue");

        JobId jobId = BackgroundJobRequest.enqueue(new TestMDCJobRequest("someKey"));
        await().atMost(30, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/581")
    void mdcContextIsAvailableInJobAndAllowsNullValues() {
        MDC.put("keyA", "keyAValue");
        MDC.put("keyB", null);

        JobId jobId = BackgroundJobRequest.enqueue(new TestMDCJobRequest("keyA"));
        await().atMost(30, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }

    @Test
    void mdcContextIsAvailableForDisplayName() {
        MDC.put("customer.id", "1");

        JobId jobId = BackgroundJobRequest.enqueue(new TestMDCJobRequest("someKey"));
        assertThat(storageProvider.getJobById(jobId)).hasJobName("Doing some hard work for customerId: 1");
    }

    @Test
    void testJobContextIsThreadSafe() {
        JobId jobId1 = BackgroundJobRequest.enqueue(new TestJobContextJobRequest());
        JobId jobId2 = BackgroundJobRequest.enqueue(new TestJobContextJobRequest());

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId1).getState() == FAILED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId2).getState() == FAILED);

        Job job1ById = storageProvider.getJobById(jobId1);
        assertThat(job1ById)
                .hasMetadata(allValuesAre(jobId1.asUUID()))
                .hasMetadata(noValueMatches(jobId2.asUUID()));

        Job job2ById = storageProvider.getJobById(jobId2);
        assertThat(job2ById)
                .hasMetadata(allValuesAre(jobId2.asUUID()))
                .hasMetadata(noValueMatches(jobId1.asUUID()));
    }

    Condition<Map<String, Object>> noValueMatches(UUID id) {
        return new Condition<>(s -> !s.containsValue(id), "a value matches %s", id);
    }

    Condition<Map<String, Object>> allValuesAre(UUID id) {
        return new Condition<>(s -> new HashSet<>(s.values()).size() == 2 && new HashSet<>(s.values()).contains(id), "a value matches %s", id);
    }

    private Stream<JobRequest> getWorkStream() {
        return Stream.of(
                new TestJobRequest("Workstream item 1"),
                new TestJobRequest("Workstream item 2"),
                new TestJobRequest("Workstream item 3"),
                new TestJobRequest("Workstream item 4"),
                new TestJobRequest("Workstream item 5")
        );
    }
}
