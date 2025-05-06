package org.jobrunr.server;

import ch.qos.logback.Logback.TemporarilyLogLevelChange;
import ch.qos.logback.classic.Level;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.strategy.WorkDistributionStrategy;
import org.jobrunr.server.tasks.steward.OnboardNewWorkTask;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.Mocks;
import org.jobrunr.utils.SleepUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import static ch.qos.logback.Logback.temporarilyChangeLogLevel;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.storage.Paging.AmountBasedList.ascOnUpdatedAt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStewardTest {

    private BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private WorkDistributionStrategy workDistributionStrategy;

    private JobSteward jobSteward;

    @BeforeEach
    void setUpBackgroundJobZooKeeper() {
        jobSteward = initializeBackgroundJobServerWithJobSteward();
    }

    @Test
    void jobsThatAreProcessedAreBeingUpdatedWithAHeartbeat() {
        jobSteward = initializeBackgroundJobServerWithJobSteward();

        final Job job = anEnqueuedJob().withId().build();
        job.startProcessingOn(backgroundJobServer);
        jobSteward.startProcessing(job, mock(Thread.class));
        jobSteward.run();

        verify(storageProvider).save(singletonList(job));
        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void checkForEnqueuedJobsIfJobsPresentSubmitsThemToTheBackgroundJobServer() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);

        lenient().when(storageProvider.getJobsToProcess(eq(backgroundJobServer), any())).thenReturn(jobs);

        jobSteward.run();

        verify(backgroundJobServer).processJob(enqueuedJob);
    }

    @Test
    void onThreadIdleNewWorkIsOnboardedAndThreadSafe() throws InterruptedException {
        final Job enqueuedJob = anEnqueuedJob().build();
        final List<Job> jobs = List.of(enqueuedJob);
        lenient().when(storageProvider.getJobsToProcess(eq(backgroundJobServer), any())).thenReturn(jobs);

        final List<Throwable> throwables = new CopyOnWriteArrayList<>();
        UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> throwables.add(throwable);

        Random random = new Random();

        final int concurrency = 10_000;
        CountDownLatch countDownLatch = new CountDownLatch(concurrency);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < concurrency; i++) {
            final Thread thread = new Thread(() -> {
                SleepUtils.sleep(random.nextInt(10));
                jobSteward.notifyThreadIdle();
                countDownLatch.countDown();
            });
            thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
            threads.add(thread);
        }

        try (TemporarilyLogLevelChange ignored = temporarilyChangeLogLevel(OnboardNewWorkTask.class, Level.INFO)) {
            threads.forEach(Thread::start);
            countDownLatch.await();
        }

        assertThat(throwables).isEmpty();
        verify(backgroundJobServer, times(concurrency)).isRunning();
        verify(backgroundJobServer, atLeast(1)).processJob(enqueuedJob); // due to ReentrantLock
    }

    private JobSteward initializeBackgroundJobServerWithJobSteward() {
        when(backgroundJobServer.isRunning()).thenReturn(true);
        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getWorkDistributionStrategy()).thenReturn(workDistributionStrategy);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters());
        lenient().when(workDistributionStrategy.canOnboardNewWork()).thenReturn(true);
        lenient().when(workDistributionStrategy.getWorkPageRequest()).thenReturn(ascOnUpdatedAt(10));
        lenient().when(backgroundJobServer.isAnnounced()).thenReturn(true);
        lenient().when(backgroundJobServer.isMaster()).thenReturn(true);
        JobSteward jobSteward = new JobSteward(backgroundJobServer);
        when(backgroundJobServer.getJobSteward()).thenReturn(jobSteward);
        return jobSteward;
    }


}