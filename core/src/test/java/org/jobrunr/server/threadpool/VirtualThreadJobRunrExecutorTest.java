package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VirtualThreadJobRunrExecutorTest {

    @Mock
    ExecutorService executorService;

    @Test
    void ifNotStartedJobsAreNotAccepted() {
        VirtualThreadJobRunrExecutor jobRunrExecutor = new VirtualThreadJobRunrExecutor(8, executorService);

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verifyNoInteractions(executorService);
    }

    @Test
    void ifStoppedJobsAreNotAccepted() {
        VirtualThreadJobRunrExecutor jobRunrExecutor = new VirtualThreadJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();
        jobRunrExecutor.stop(Duration.ofSeconds(10));

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verify(executorService).shutdown();
        verify(executorService, never()).submit(any(Runnable.class));
    }

    @Test
    void ifStartedJobsAreAccepted() {
        VirtualThreadJobRunrExecutor jobRunrExecutor = new VirtualThreadJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verify(executorService).submit(any(Runnable.class));
    }
}