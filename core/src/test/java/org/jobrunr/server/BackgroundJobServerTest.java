package org.jobrunr.server;

import org.jobrunr.JobRunrException;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.lambdas.IocJobLambda;
import org.jobrunr.jobs.stubs.SimpleJobActivator;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.runner.BackgroundJobWithIocRunner;
import org.jobrunr.server.runner.BackgroundJobWithoutIocRunner;
import org.jobrunr.storage.SimpleStorageProvider;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.TestService;
import org.jobrunr.stubs.TestServiceForIoC;
import org.jobrunr.stubs.TestServiceThatCannotBeRun;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.FIVE_SECONDS;
import static org.awaitility.Durations.TEN_SECONDS;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.jobs.states.StateName.PROCESSING;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;

public class BackgroundJobServerTest {

    private TestService testService;
    private StorageProvider storageProvider;
    private BackgroundJobServer backgroundJobServer;
    private TestServiceForIoC testServiceForIoC;

    @BeforeEach
    public void setUpTestService() {
        testService = new TestService();
        testServiceForIoC = new TestServiceForIoC("an argument");
        testService.reset();
        testServiceForIoC.reset();
        storageProvider = new SimpleStorageProvider();
        backgroundJobServer = new BackgroundJobServer(storageProvider, new SimpleJobActivator(testServiceForIoC));
        JobRunr.configure()
                .useStorageProvider(storageProvider)
                .useBackgroundJobServer(backgroundJobServer)
                .initialize();
    }

    @AfterEach
    public void stopBackgroundJobServer() {
        backgroundJobServer.stop();
    }

    @Test
    public void testStartAndStop() {
        // GIVEN server stopped and we enqueue a job
        UUID jobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(FIVE_SECONDS).atMost(TEN_SECONDS).until(() -> testService.getProcessedJobs() == 0);
        assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED);

        // WHEN we start the server
        backgroundJobServer.start();

        // THEN the job should be processed
        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(storageProvider.getJobById(jobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED));

        // WHEN we pause the server and enqueue a new job
        backgroundJobServer.pauseProcessing();
        UUID anotherJobId = BackgroundJob.enqueue(() -> testService.doWork());

        // THEN the job should stay in state ENQUEUED
        await().during(FIVE_SECONDS).atMost(TEN_SECONDS).until(() -> testService.getProcessedJobs() == 1);
        assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED);

        // WHEN we resume the server again
        backgroundJobServer.resumeProcessing();

        // THEN the job should be processed again
        await().atMost(TEN_SECONDS).until(() -> testService.getProcessedJobs() > 1);
        assertThat(storageProvider.getJobById(anotherJobId)).hasStates(ENQUEUED, PROCESSING, SUCCEEDED);

        // WHEN we shutdown the server
        backgroundJobServer.stop();

        // THEN no running backgroundjob threads should exist
        await().atMost(TEN_SECONDS)
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
    }

    @Test
    public void testOnServerExitCleansUpAllThreads() {
        final int amountOfJobs = 10;

        Map<Thread, StackTraceElement[]> stackTracesBeforeStart = Thread.getAllStackTraces();

        backgroundJobServer.start();
        for (int i = 0; i < amountOfJobs; i++) {
            BackgroundJob.enqueue(() -> testService.doWork());
        }
        await().atMost(TEN_SECONDS).until(() -> storageProvider.countJobs(SUCCEEDED) == amountOfJobs);

        await().atMost(TEN_SECONDS).untilAsserted(() -> assertThat(Thread.getAllStackTraces()).matches(this::containsBackgroundJobThreads));

        backgroundJobServer.stop();
        await().atMost(TEN_SECONDS)
                .untilAsserted(() -> assertThat(Thread.getAllStackTraces())
                        .matches(this::containsNoBackgroundJobThreads, "Found BackgroundJob Threads: \n\t" + getThreadNames(Thread.getAllStackTraces()).collect(Collectors.joining("\n\t"))));
    }

    @Test
    public void getBackgroundJobRunnerForIoCJobWithoutInstance() {
        final Job job = anEnqueuedJob()
                .withJobDetails((IocJobLambda<TestServiceForIoC>) (x) -> x.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithIocRunner.class);
    }

    @Test
    public void getBackgroundJobRunnerForIoCJobWithInstance() {
        final Job job = anEnqueuedJob()
                .withJobDetails(() -> testServiceForIoC.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithIocRunner.class);
    }

    @Test
    public void getBackgroundJobRunnerForNonIoCJobWithoutInstance() {
        final Job job = anEnqueuedJob()
                .withJobDetails((IocJobLambda<TestService>) (x) -> x.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithoutIocRunner.class);
    }

    @Test
    public void getBackgroundJobRunnerForNonIoCJobWithInstance() {
        final Job job = anEnqueuedJob()
                .withJobDetails(() -> testService.doWork())
                .build();
        assertThat(backgroundJobServer.getBackgroundJobRunner(job))
                .isNotNull()
                .isInstanceOf(BackgroundJobWithoutIocRunner.class);
    }

    @Test
    public void getBackgroundJobRunnerForJobThatCannotBeRun() {
        final Job job = anEnqueuedJob()
                .withJobDetails((IocJobLambda<TestServiceThatCannotBeRun>) (x) -> x.doWork())
                .build();
        assertThatThrownBy(() -> backgroundJobServer.getBackgroundJobRunner(job))
                .isInstanceOf(JobRunrException.class);
    }

    private boolean containsNoBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).noneMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private boolean containsBackgroundJobThreads(Map<Thread, StackTraceElement[]> threadMap) {
        return getThreadNames(threadMap).anyMatch(threadName -> threadName.startsWith("backgroundjob"));
    }

    private Stream<String> getThreadNames(Map<Thread, StackTraceElement[]> threadMap) {
        return threadMap.keySet().stream().map(thread -> thread.getName());
    }

}
