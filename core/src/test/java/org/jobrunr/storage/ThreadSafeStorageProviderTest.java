package org.jobrunr.storage;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.time.Duration.between;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.utils.SleepUtils.sleep;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ThreadSafeStorageProviderTest {

    @Mock
    private StorageProvider storageProviderMock;
    private ThreadSafeStorageProvider threadSafeStorageProvider;

    @BeforeEach
    void setUp() {
        threadSafeStorageProvider = new ThreadSafeStorageProvider(storageProviderMock);

        lenient().when(storageProviderMock.save(any(Job.class))).thenAnswer(invocation -> {
            sleep(100);
            return invocation.getArgument(0);
        });
        lenient().when(storageProviderMock.save(any(List.class))).thenAnswer(invocation -> {
            sleep(100);
            return invocation.getArgument(0);
        });
    }

    @Test
    void multipleJobsCanBeSavedConcurrently() throws InterruptedException {
        final Job succeededJob1 = aSucceededJob().build();
        final Job succeededJob2 = aSucceededJob().build();
        final Job succeededJob3 = aSucceededJob().build();
        final Job failedJob = aFailedJob().build();

        CountDownLatch countDownLatch = new CountDownLatch(4);

        final ExecutorService executorService = Executors.newFixedThreadPool(4);
        final Callable<Void> runnable1 = () -> saveAndCountDown(succeededJob1, countDownLatch);
        final Callable<Void> runnable2 = () -> saveAndCountDown(succeededJob2, countDownLatch);
        final Callable<Void> runnable3 = () -> saveAndCountDown(succeededJob3, countDownLatch);
        final Callable<Void> runnable4 = () -> saveAndCountDown(failedJob, countDownLatch);

        final Instant before = Instant.now();
        executorService.invokeAll(asList(
                runnable1,
                runnable2,
                runnable3,
                runnable4
        ));
        countDownLatch.await();
        final Instant after = Instant.now();

        assertThat(between(before, after).toMillis()).isLessThan(250L);
    }

    @Test
    void sameJobCanNotBeSavedConcurrently() throws InterruptedException {
        final Job jobInProgress1 = aJobInProgress().build();
        final Job jobInProgress2 = aJobInProgress().build();
        final Job finishedJob = aCopyOf(jobInProgress1).withSucceededState().build();

        CountDownLatch countDownLatch = new CountDownLatch(2);

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final Callable<Void> runnable1 = () -> saveAllAndCountDown(asList(jobInProgress1, jobInProgress2), countDownLatch);
        final Callable<Void> runnable2 = () -> saveAndCountDown(finishedJob, countDownLatch);

        final Instant before = Instant.now();
        executorService.invokeAll(asList(
                runnable1,
                runnable2
        ));
        countDownLatch.await();
        final Instant after = Instant.now();

        assertThat(between(before, after).toMillis()).isGreaterThan(200L);
    }

    private Void saveAndCountDown(Job job, CountDownLatch countDownLatch) {
        threadSafeStorageProvider.save(job);
        countDownLatch.countDown();
        return null;
    }

    private Void saveAllAndCountDown(List<Job> jobs, CountDownLatch countDownLatch) {
        threadSafeStorageProvider.save(jobs);
        countDownLatch.countDown();
        return null;
    }
}