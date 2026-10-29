package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.utils.ThreadUtils.assertNoBackgroundJobServerThreadsExist;

class PlatformThreadPoolJobRunrExecutorTest {

    PlatformThreadPoolJobRunrExecutor platformThreadPoolJobRunrExecutor;

    @BeforeEach
    void setUp() {
        platformThreadPoolJobRunrExecutor = new PlatformThreadPoolJobRunrExecutor(2);
    }

    @AfterEach
    void tearDown() {
        platformThreadPoolJobRunrExecutor.stop(Duration.ZERO);
        assertNoBackgroundJobServerThreadsExist();
    }

    @Test
    void testPlatformThreadPoolJobRunrExecutor() {
        assertThat(platformThreadPoolJobRunrExecutor).hasExecutorOfType(ScheduledThreadPoolExecutor.class);
        assertThat(platformThreadPoolJobRunrExecutor.executorService.getClass().getSimpleName()).isEqualTo("ScheduledThreadPoolExecutor");
    }

    @Test
    void testPlatformThreadPoolJobRunrExecutorCanCancelTasks() {
        // GIVEN
        MyRunnable myRunnable1 = new MyRunnable(1);
        MyRunnable myRunnable2 = new MyRunnable(2);

        // WHEN
        platformThreadPoolJobRunrExecutor.scheduleWithFixedDelay(myRunnable1, Duration.ZERO, Duration.ofSeconds(1));
        platformThreadPoolJobRunrExecutor.scheduleWithFixedDelay(myRunnable2, Duration.ZERO, Duration.ofSeconds(1));

        // THEN
        await().until(() -> myRunnable1.invocationCount == 1 && myRunnable2.invocationCount == 1);

        // WHEN
        platformThreadPoolJobRunrExecutor.cancelScheduledFuturesOfType(MyRunnable.class);

        // THEN
        await().during(3, TimeUnit.SECONDS).until(() -> myRunnable1.invocationCount == 1 && myRunnable2.invocationCount == 1);
        assertThat(platformThreadPoolJobRunrExecutor).hasExecutorWithCorePoolSize(2);
    }

    @Test
    void testPlatformThreadPoolJobRunrExecutorCanCancelTasksAndReducePoolSize() {
        // GIVEN
        MyRunnable myRunnable1 = new MyRunnable(1);
        MyRunnable myRunnable2 = new MyRunnable(2);

        // WHEN
        platformThreadPoolJobRunrExecutor.increasePoolSize(2);
        platformThreadPoolJobRunrExecutor.scheduleWithFixedDelay(myRunnable1, Duration.ZERO, Duration.ofSeconds(1));
        platformThreadPoolJobRunrExecutor.scheduleWithFixedDelay(myRunnable2, Duration.ZERO, Duration.ofSeconds(1));

        // THEN
        assertThat(platformThreadPoolJobRunrExecutor).hasExecutorWithCorePoolSize(4);
        await().until(() -> myRunnable1.invocationCount == 1 && myRunnable2.invocationCount == 1);

        // WHEN
        platformThreadPoolJobRunrExecutor.cancelScheduledFuturesOfType(MyRunnable.class);

        // THEN
        await().during(3, TimeUnit.SECONDS).until(() -> myRunnable1.invocationCount == 1 && myRunnable2.invocationCount == 1);
        assertThat(platformThreadPoolJobRunrExecutor).hasExecutorWithCorePoolSize(2);
    }

    private static class MyRunnable implements Runnable {
        private final int number;
        private int invocationCount;

        private MyRunnable(int number) {
            this.number = number;
            this.invocationCount = 0;
        }

        @Override
        public void run() {
            invocationCount++;
            System.out.println("MyRunnable " + number + ": run " + invocationCount);
        }
    }
}