package org.jobrunr.scheduling;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.Duration.ofSeconds;
import static java.time.Instant.now;
import static java.time.ZoneId.systemDefault;
import static java.util.stream.Collectors.toSet;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.PageRequest.ascOnUpdatedAt;

public class BackgroundJobByJobRequestTest {

    private StorageProviderForTest storageProvider;
    private BackgroundJobServer backgroundJobServer;

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
        backgroundJobServer.stop();
        storageProvider.close();
    }

    @Test
    void testEnqueue() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueue"));
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
    void testEnqueueAlsoTakesIntoAccountJobFilters() {
        //TODO
    }

    @Test
    void testEnqueueOfFailingJobAndRetryCount() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueue", true));
        await().atMost(15, TimeUnit.SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == FAILED);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED, ENQUEUED, PROCESSING, FAILED);
    }

    @Test
    void testEnqueueWithJobContextAndMetadata() {
        JobId jobId = BackgroundJobRequest.enqueue(new TestJobRequest("from testEnqueueWithJobContextAndMetadata"));
        await().atMost(FIVE_SECONDS).until(() -> storageProvider.getJobById(jobId).getState() == SUCCEEDED);
        Job jobById = storageProvider.getJobById(jobId);
        assertThat(jobById)
                .hasStates(ENQUEUED, PROCESSING, SUCCEEDED)
                .hasMetadata("test", "test");
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
    void testRecurringJob() {
        BackgroundJobRequest.scheduleRecurrently(Cron.minutely(), new TestJobRequest("from testRecurringJob"));
        await().atMost(ofSeconds(65)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringJobWithId() {
        BackgroundJobRequest.scheduleRecurrently("theId", Cron.minutely(), new TestJobRequest("from testRecurringJobWithId"));
        await().atMost(ofSeconds(65)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testRecurringJobWithIdAndTimezone() {
        BackgroundJobRequest.scheduleRecurrently("theId", Cron.minutely(), systemDefault(), new TestJobRequest("from testRecurringJobWithIdAndTimezone"));
        await().atMost(ofSeconds(65)).until(() -> storageProvider.countJobs(SUCCEEDED) == 1);

        final Job job = storageProvider.getJobs(SUCCEEDED, ascOnUpdatedAt(1000)).get(0);
        assertThat(storageProvider.getJobById(job.getId())).hasStates(SCHEDULED, ENQUEUED, PROCESSING, SUCCEEDED);
    }

    @Test
    void testJobContextIsThreadSafe() {
        JobId jobId1 = BackgroundJobRequest.enqueue(new TestJobContextJobRequest());
        JobId jobId2 = BackgroundJobRequest.enqueue(new TestJobContextJobRequest());

        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId1).getState() == SUCCEEDED);
        await().atMost(TEN_SECONDS).until(() -> storageProvider.getJobById(jobId2).getState() == SUCCEEDED);

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
