package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractJobRunrExecutorTest {

    @Mock
    ExecutorService executorService;

    @Test
    void ifNotStartedJobsAreNotAccepted() {
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verifyNoInteractions(executorService);
    }

    @Test
    void ifStoppedJobsAreNotAccepted() {
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();
        jobRunrExecutor.stop(Duration.ofSeconds(10));

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verify(executorService).shutdown();
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    void ifStartedJobsAreAccepted() {
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verify(executorService).execute(any(Runnable.class));
    }

    @Test
    void stopShutdownGracefully() throws InterruptedException {
        // Arrange
        when(executorService.awaitTermination(anyLong(), any())).thenReturn(true);
        Duration timeout = Duration.ofSeconds(5); // 4s for jobs, 1s for save

        // Act
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);
        jobRunrExecutor.stop(timeout);

        // Assert
        InOrder inOrder = inOrder(executorService);
        inOrder.verify(executorService).shutdown();
        inOrder.verify(executorService).awaitTermination(4000, TimeUnit.MILLISECONDS);
        inOrder.verify(executorService).awaitTermination(1000, TimeUnit.MILLISECONDS);

        // shutdownNow should NEVER be called if awaitTermination returns true
        verify(executorService, never()).shutdownNow();
    }

    @Test
    void stopCallsShutdownNowAndExtraAwaitOnTimeout() throws InterruptedException {
        // GIVEN
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class)))
                .thenReturn(false) // First call (awaitTimeout)
                .thenReturn(true);  // Second call (1s grace)

        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);
        Duration timeout = Duration.ofSeconds(5);

        // WHEN
        jobRunrExecutor.stop(timeout);

        // THEN
        InOrder inOrder = inOrder(executorService);
        inOrder.verify(executorService).shutdown();
        inOrder.verify(executorService).awaitTermination(4000, TimeUnit.MILLISECONDS);
        inOrder.verify(executorService).shutdownNow();
        inOrder.verify(executorService).awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    void stopHandlesShortTimeouts() throws InterruptedException {
        // GIVEN
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);

        // WHEN
        jobRunrExecutor.stop(Duration.ofMillis(500)); // Less than 1s

        // THEN
        verify(executorService).awaitTermination(0, TimeUnit.MILLISECONDS);
        verify(executorService).awaitTermination(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    void stopShouldHandleInterruptedException() throws InterruptedException {
        // GIVEN
        AbstractJobRunrExecutor<?> jobRunrExecutor = createJobRunrExecutor(8, executorService);
        when(executorService.awaitTermination(anyLong(), any())).thenThrow(new InterruptedException());

        // Act
        jobRunrExecutor.stop(Duration.ofSeconds(5));

        // Assert
        verify(executorService).shutdownNow();
        assertThat(Thread.interrupted()).isTrue(); // Verify flag is reset
    }

    private AbstractJobRunrExecutor<?> createJobRunrExecutor(int workerCount, ExecutorService executorService) {
        return new AbstractJobRunrExecutor(workerCount, executorService) {};
    }
}