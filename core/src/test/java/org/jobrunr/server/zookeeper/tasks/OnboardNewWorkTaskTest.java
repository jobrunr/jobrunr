package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.ENQUEUED;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OnboardNewWorkTaskTest extends AbstractZooKeeperTaskTest {

    OnboardNewWorkTask task;

    @BeforeEach
    void setUpTask() {
        task = new OnboardNewWorkTask(jobZooKeeper, backgroundJobServer);
        when(backgroundJobServer.isRunning()).thenReturn(true);
    }

    @Test
    void testTask() {
        Job enqueuedJob1 = anEnqueuedJob().build();
        Job enqueuedJob2 = anEnqueuedJob().build();
        when(storageProvider.getJobs(eq(ENQUEUED), any())).thenReturn(asList(enqueuedJob1, enqueuedJob2), emptyJobList());
        runTask(task);

        verify(backgroundJobServer).processJob(enqueuedJob1);
        verify(backgroundJobServer).processJob(enqueuedJob2);
    }

    @Test
    void taskIsNotDoneConcurrently() throws InterruptedException {
        when(storageProvider.getJobs(eq(ENQUEUED), any())).thenAnswer((invocationOnMock) -> {
            sleep(100);
            return emptyList();
        });

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
        verify(storageProvider, times(1)).getJobs(eq(ENQUEUED), any());
    }
}