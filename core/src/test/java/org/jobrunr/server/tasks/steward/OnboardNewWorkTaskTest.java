package org.jobrunr.server.tasks.steward;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.storage.StorageException;
import org.jobrunr.utils.SleepUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OnboardNewWorkTaskTest extends AbstractTaskTest {

    OnboardNewWorkTask task;

    @BeforeEach
    void setUpTask() {
        task = new OnboardNewWorkTask(backgroundJobServer);
        when(backgroundJobServer.isRunning()).thenReturn(true);
    }

    @Test
    void testTask() {
        Job enqueuedJob1 = anEnqueuedJob().build();
        Job enqueuedJob2 = anEnqueuedJob().build();

        saveJobsInStorageProvider(enqueuedJob1, enqueuedJob2);

        runTask(task);

        verify(backgroundJobServer, times(2)).processJob(any(Job.class));
    }

    @Test
    void testTaskCanHappenAgainAfterException() {
        Job enqueuedJob1 = anEnqueuedJob().build();
        Job enqueuedJob2 = anEnqueuedJob().build();

        doThrow(new StorageException("Some error occurred"))
                .doReturn(asList(enqueuedJob1, enqueuedJob2), emptyList())
                .when(storageProvider).getJobsToProcess(eq(backgroundJobServer), any());

        new Thread(() -> runTask(task)).start();
        SleepUtils.sleep(500);

        runTask(task);

        verify(backgroundJobServer).processJob(enqueuedJob1);
        verify(backgroundJobServer).processJob(enqueuedJob2);
    }

    @Test
    void taskIsNotDoneConcurrentlyBecauseOfTheReentrantLock() throws InterruptedException {
        doAnswer(invocation -> {
            sleep(100);
            return emptyList();
        }).when(storageProvider).getJobsToProcess(eq(backgroundJobServer), any());

        CountDownLatch countDownLatch = new CountDownLatch(2);
        final Thread thread1 = new Thread(() -> {
            runTask(task);
            countDownLatch.countDown();
        });
        final Thread thread2 = new Thread(() -> {
            runTask(task);
            countDownLatch.countDown();
        });
        thread1.start();
        thread2.start();

        countDownLatch.await();
        verify(storageProvider, times(1)).getJobsToProcess(eq(backgroundJobServer), any());
    }
}