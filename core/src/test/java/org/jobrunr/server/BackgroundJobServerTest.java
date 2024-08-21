package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.JobRunrMetadata;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.sql.h2.H2StorageProvider;
import org.jobrunr.stubs.StaticTestService;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForIoC;
import org.jobrunr.stubs.TestServiceThatCannotBeRun;
import org.jobrunr.utils.SleepUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.ThrowsException;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.time.Duration.ofMillis;
import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.ONE_MINUTE;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.awaitility.Durations.TWO_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.jobrunr.storage.StorageProviderUtils.DatabaseOptions.NO_VALIDATE;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class BackgroundJobServerTest {

    private TestService testService;
    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private TestServiceForIoC testServiceForIoC;
    private SimpleJobActivator jobActivator;
    private ListAppender<ILoggingEvent> logger;
    private ListAppender<ILoggingEvent> jobZooKeeperLogger;

    @BeforeEach
    void setUp() {
        testService = new TestService();
        testServiceForIoC = new TestServiceForIoC("an argument");
        testService.reset();
        testServiceForIoC.reset();
        storageProvider = Mockito.spy(new InMemoryStorageProvider());
        jobActivator = new SimpleJobActivator(testServiceForIoC);
        JobRunr.configure()
                .useJobActivator(jobActivator)
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(500)), false)
                .initialize();
        backgroundJobServer = JobRunr.getBackgroundJobServer();
        logger = LoggerAssert.initFor(backgroundJobServer);
        jobZooKeeperLogger = LoggerAssert.initFor(backgroundJobServer.getJobSteward());
    }

    @AfterEach
    void stopBackgroundJobServer() {
        backgroundJobServer.stop();
    }

    @Test
    void backgroundJobServerWaitsForMigrationBeforeBeingAnnounced() {
        doAnswer(invocation -> {
            // simulate long during migration
            JobRunrMetadata metadata = invocation.getArgument(0);
            if ("database_version".equals(metadata.getName()) && "6.0.0".equals(metadata.getValue())) {
                sleep(5, SECONDS);
            }
            return invocation.callRealMethod();
        }).when(storageProvider).saveMetadata(any());

        // WHEN
        backgroundJobServer.start();

        // THEN
        sleep(100, MILLISECONDS);
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isNotReadyToProcessJobs()).isTrue();

        // WHEN migration is running
        await().during(4, SECONDS)
                .until(() -> backgroundJobServer.isNotReadyToProcessJobs());

        // THEN
        await().atMost(2, SECONDS)
                .untilAsserted(() -> assertThat(backgroundJobServer.isNotReadyToProcessJobs()).isFalse());
    }

    @Test
    void backgroundJobServerValidatesPollIntervalInSecondsAndThrowsExceptionIfTooSmall() {
        assertThatThrownBy(() -> {
            JobRunr.configure()
                    .useStorageProvider(new H2StorageProvider(null, NO_VALIDATE))
                    .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollInterval(ofMillis(500)), false)
                    .initialize();
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The smallest supported pollInterval is 5 seconds - otherwise it will cause to much load on your SQL/noSQL datastore.");
    }

    @Test
    void testStartAndStop() {
        // GIVEN server stopped and we enqueue a job
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(TWO_SECONDS).until(() -> testService.getProcessedJobs() == 0);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED);

        // WHEN we start the server
        backgroundJobServer.start();

        // THEN the job should be processed
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));

        // WHEN we pause the server and enqueue a new job
        backgroundJobServer.pauseProcessing();
        JobId anotherJobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(1, SECONDS).until(() -> testService.getProcessedJobs() == 1);
        await().during(1, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED));

        // WHEN we resume the server again
        backgroundJobServer.resumeProcessing();

        // THEN the job should be processed again
        await().atMost(FIVE_SECONDS).until(() -> testService.getProcessedJobs() > 1);
        await().atMost(FIVE_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));

        // WHEN we shutdown the server
        backgroundJobServer.stop();
        assertThat(logger).hasInfoMessageContaining("BackgroundJobServer and BackgroundJobPerformers - stopping (waiting for all jobs to complete - max 10 seconds)", 1);

        // THEN no running backgroundjob threads should exist
        await().atMost(TEN_SECONDS)
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
        assertThat(logger).hasInfoMessageContaining("BackgroundJobServer and BackgroundJobPerformers stopped", 1);
        assertThat(jobZooKeeperLogger).hasNoWarnLogMessages();
    }

    @Test
    void testOnServerExitCleansUpAllThreads() {
        final int amountOfJobs = 10;

        backgroundJobServer.start();
        for (int i = 0; i < amountOfJobs; i++) {
            BackgroundJob.enqueue(() -> testService.doWork());
        }
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(storageProvider).hasJobs(SUCCEEDED, amountOfJobs));
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(Thread.getAllStackTraces()).matches(this::containsBackgroundJobThreads));

        backgroundJobServer.stop();
        await().atMost(ONE_MINUTE)
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
    }

    @Test
    void testServerStatusStateMachine() {
        // INITIAL
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isUnAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();

        // INITIAL -> START (with failure)
        doAnswer(new AnswersWithDelay(100, new ThrowsException(new IllegalStateException()))).when(storageProvider).announceBackgroundJobServer(any());
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isUnAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
        await().until(() -> backgroundJobServer.isStopped());
        Mockito.reset(storageProvider);

        // INITIAL -> START
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        await().until(() -> backgroundJobServer.isAnnounced() == true);
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();

        // START -> PAUSE
        assertThatCode(() -> backgroundJobServer.pauseProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isFalse();

        // PAUSE -> PAUSE
        assertThatCode(() -> backgroundJobServer.pauseProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isFalse();
        assertThat(backgroundJobServer.isStopped()).isFalse();

        // PAUSE -> STOP
        assertThatCode(() -> backgroundJobServer.stop()).doesNotThrowAnyException();
        await().until(() -> backgroundJobServer.isAnnounced() == false);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();
        assertThat(backgroundJobServer.isStopped()).isTrue();

        // STOP -> RESUME
        assertThatThrownBy(() -> backgroundJobServer.resumeProcessing()).isInstanceOf(IllegalStateException.class);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();
        assertThat(backgroundJobServer.isStopped()).isTrue();

        // STOP -> PAUSE
        assertThatThrownBy(() -> backgroundJobServer.pauseProcessing()).isInstanceOf(IllegalStateException.class);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();
        assertThat(backgroundJobServer.isStopped()).isTrue();

        // STOP -> START
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        await().until(() -> backgroundJobServer.isAnnounced() == true);
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
        assertThat(backgroundJobServer.isStopped()).isFalse();

        // START -> START
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
        assertThat(backgroundJobServer.isStopped()).isFalse();

        // START -> RESUME
        assertThatCode(() -> backgroundJobServer.resumeProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
        assertThat(backgroundJobServer.isStopped()).isFalse();

        // RESUME -> RESUME
        assertThatCode(() -> backgroundJobServer.resumeProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
    }

    @Test
    void testStopBackgroundJobServerWhileProcessing() {
        backgroundJobServer.start();

        final JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(12));
        await().atMost(500, MILLISECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));
        backgroundJobServer.stop();
        await().atMost(20, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(FAILED) || storageProvider.getJobById(jobId).hasState(SCHEDULED));
        backgroundJobServer.start();
        await().atMost(25, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
    }

    @Test
    void testBackgroundJobServerWasKilledWhileProcessing() {
        backgroundJobServer.start();

        final Job jobThatWasProcessedButBackgroundJobServerWasKilled = storageProvider.save(anEnqueuedJob().withState(new ProcessingState(backgroundJobServer), now().minus(2, ChronoUnit.MINUTES)).build());
        await().atMost(7, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobThatWasProcessedButBackgroundJobServerWasKilled.getId())).hasStates(ENQUEUED, PROCESSING, FAILED, SCHEDULED));
        await().atMost(7, SECONDS).until(() -> storageProvider.getJobById(jobThatWasProcessedButBackgroundJobServerWasKilled.getId()).hasState(SUCCEEDED));
    }

    @Test
    void testHeartbeatsAreSentForJobsInProcessingState() {
        backgroundJobServer.start();

        final JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(4));
        await().pollInterval(150, MILLISECONDS).pollDelay(1, SECONDS).atMost(2, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
        await().pollInterval(150, MILLISECONDS).pollDelay(1, SECONDS).atMost(2, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
        await().pollInterval(150, MILLISECONDS).pollDelay(1, SECONDS).atMost(2, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
    }

    @Test
    void testCanNotStartBackgroundJobServerTwice() {
        new Thread(() -> backgroundJobServer.start()).start();
        new Thread(() -> backgroundJobServer.start()).start();

        SleepUtils.sleep(200);

        await().until(() -> backgroundJobServer.isStarted());
        await().untilAsserted(() -> assertThat(logger).hasInfoMessageContaining("BackgroundJobPerformers started successfully", 1).hasNoErrorLogMessages());
    }

    @Test
    void getBackgroundJobRunnerForIoCJobWithoutInstance() {
        final Job job = anEnqueuedJob()
                .<TestService>withJobDetails(ts -> ts.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithIocRunner.class);
    }

    @Test
    void getBackgroundJobRunnerForIoCJobWithInstance() {
        final Job job = anEnqueuedJob()
                .withJobDetails(() -> testServiceForIoC.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithIocRunner.class);
    }

    @Test
    void getBackgroundJobRunnerForNonIoCJobWithoutInstance() {
        jobActivator.clear();

        final Job job = anEnqueuedJob()
                .<TestService>withJobDetails(ts -> ts.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithoutIocRunner.class);
    }

    @Test
    void getBackgroundJobRunnerForNonIoCJobWithInstance() {
        jobActivator.clear();

        final Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithoutIocRunner.class);
    }

    @Test
    void getBackgroundJobRunnerForNonIoCStaticJobWithoutInstance() {
        jobActivator.clear();

        final Job job = anEnqueuedJob()
                .withJobDetails(StaticTestService::doWorkInStaticMethodWithoutParameter)
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundStaticJobWithoutIocRunner.class);
    }

    @Test
    void getBackgroundJobRunnerForJobThatCannotBeRun() {
        final Job job = anEnqueuedJob()
                .<TestServiceThatCannotBeRun>withJobDetails(ts -> ts.doWork())
                .build();
        assertThatThrownBy(() -> backgroundJobServer.getBackgroundJobRunner(job))
                .isInstanceOf(JobRunrException.class);
    }

    @Test
    void ifAnnouncingBackgroundSucceedsStartupMessageIsLogged() {
        backgroundJobServer.start();

        await().atMost(10, SECONDS)
                .untilAsserted(() -> assertThat(logger)
                        .hasInfoMessageContaining("BackgroundJobPerformers started successfully")
                        .hasNoErrorLogMessages());
    }

    @Test
    void ifAnnouncingBackgroundJobServerFailsThisIsLogged() {
        Mockito.doThrow(new StorageException("Fail")).when(storageProvider).announceBackgroundJobServer(any());

        backgroundJobServer.start();

        await().atMost(10, SECONDS)
                .untilAsserted(() -> assertThat(logger).hasErrorMessage("JobRunr BackgroundJobServer failed to start"));
    }

    private boolean containsNoBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).noneMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private boolean containsBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).anyMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private Stream<String> getThreadNames(Map<Thread, StackTraceElement[]> threadMap) {
        return threadMap.keySet().stream().map(Thread::getName);
    }

}
