package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.server.runner.BackgroundStaticJobWithoutIocRunner;
import org.jobrunr.storage.InMemoryStorageProvider;
import org.jobrunr.storage.StorageException;
import org.jobrunr.storage.StorageProvider;
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

import static java.time.Instant.now;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.*;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.*;
import static org.jobrunr.server.BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

class BackgroundJobServerTest {

    private TestService testService;
    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private TestServiceForIoC testServiceForIoC;
    private SimpleJobActivator jobActivator;
    private ListAppender<ILoggingEvent> logger;

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
                .useBackgroundJobServer(usingStandardBackgroundJobServerConfiguration().andPollIntervalInSeconds(5), false)
                .initialize();
        backgroundJobServer = JobRunr.getBackgroundJobServer();
        logger = LoggerAssert.initFor(backgroundJobServer);
    }

    @AfterEach
    void stopBackgroundJobServer() {
        backgroundJobServer.stop();
    }

    @Test
    void testStartAndStop() {
        // GIVEN server stopped and we enqueue a job
        JobId jobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(TWO_SECONDS).atMost(FIVE_SECONDS).until(() -> testService.getProcessedJobs() == 0);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED);

        // WHEN we start the server
        backgroundJobServer.start();

        // THEN the job should be processed
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));

        // WHEN we pause the server and enqueue a new job
        backgroundJobServer.pauseProcessing();
        JobId anotherJobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(TWO_SECONDS).atMost(FIVE_SECONDS).until(() -> testService.getProcessedJobs() == 1);
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED));

        // WHEN we resume the server again
        backgroundJobServer.resumeProcessing();

        // THEN the job should be processed again
        await().atMost(TEN_SECONDS).until(() -> testService.getProcessedJobs() > 1);
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));

        // WHEN we shutdown the server
        backgroundJobServer.stop();
        assertThat(logger).hasInfoMessageContaining("BackgroundJobServer and BackgroundJobPerformers - stopping (waiting for all jobs to complete - max 10 seconds)", 1);

        // THEN no running backgroundjob threads should exist
        await().atMost(TEN_SECONDS)
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
        assertThat(logger).hasInfoMessageContaining("BackgroundJobServer and BackgroundJobPerformers stopped", 1);
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

        // PAUSE -> STOP
        assertThatCode(() -> backgroundJobServer.stop()).doesNotThrowAnyException();
        await().until(() -> backgroundJobServer.isAnnounced() == false);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();

        // STOP -> RESUME
        assertThatThrownBy(() -> backgroundJobServer.resumeProcessing()).isInstanceOf(IllegalStateException.class);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();

        // STOP -> PAUSE
        assertThatThrownBy(() -> backgroundJobServer.pauseProcessing()).isInstanceOf(IllegalStateException.class);
        assertThat(backgroundJobServer.isAnnounced()).isFalse();
        assertThat(backgroundJobServer.isStarted()).isFalse();
        assertThat(backgroundJobServer.isRunning()).isFalse();

        // STOP -> START
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        await().until(() -> backgroundJobServer.isAnnounced() == true);
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();

        // START -> START
        assertThatCode(() -> backgroundJobServer.start()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();

        // START -> RESUME
        assertThatCode(() -> backgroundJobServer.resumeProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();

        // RESUME -> RESUME
        assertThatCode(() -> backgroundJobServer.resumeProcessing()).doesNotThrowAnyException();
        assertThat(backgroundJobServer.isAnnounced()).isTrue();
        assertThat(backgroundJobServer.isStarted()).isTrue();
        assertThat(backgroundJobServer.isRunning()).isTrue();
    }

    @Test
    void testStopBackgroundJobServerWhileProcessing() {
        backgroundJobServer.start();

        final JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(15));
        await().atMost(6, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(PROCESSING));
        backgroundJobServer.stop();
        await().atMost(60, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(FAILED) || storageProvider.getJobById(jobId).hasState(SCHEDULED));
        backgroundJobServer.start();
        await().atMost(21, SECONDS).until(() -> storageProvider.getJobById(jobId).hasState(SUCCEEDED));
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

        final JobId jobId = BackgroundJob.enqueue(() -> testService.doWorkThatTakesLong(16));
        await().pollInterval(150, MILLISECONDS).pollDelay(3, SECONDS).atMost(7, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
        await().pollInterval(150, MILLISECONDS).pollDelay(3, SECONDS).atMost(7, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
        await().pollInterval(150, MILLISECONDS).pollDelay(3, SECONDS).atMost(7, SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasUpdatedAtCloseTo(now(), within(500, ChronoUnit.MILLIS)));
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
                .withJobDetails((IocJobLambda<TestServiceForIoC>) (x) -> x.doWork())
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
                .withJobDetails((IocJobLambda<TestService>) (x) -> x.doWork())
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
                .withJobDetails((IocJobLambda<TestServiceThatCannotBeRun>) (x) -> x.doWork())
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
