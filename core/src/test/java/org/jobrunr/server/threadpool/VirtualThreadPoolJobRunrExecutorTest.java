package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class VirtualThreadPoolJobRunrExecutorTest {

    @Mock
    ExecutorService executorService;

    @Test
    void ifNotStartedJobsAreNotAccepted() {
        VirtualThreadPoolJobRunrExecutor jobRunrExecutor = new VirtualThreadPoolJobRunrExecutor(8, executorService);

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verifyNoInteractions(executorService);
    }

    @Test
    void ifStoppedJobsAreNotAccepted() {
        VirtualThreadPoolJobRunrExecutor jobRunrExecutor = new VirtualThreadPoolJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();
        jobRunrExecutor.stop();

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verifyNoInteractions(executorService);
    }

    @Test
    void ifStartedJobsAreAccepted() {
        VirtualThreadPoolJobRunrExecutor jobRunrExecutor = new VirtualThreadPoolJobRunrExecutor(8, executorService);
        jobRunrExecutor.start();

        jobRunrExecutor.execute(() -> System.out.println("A Runnable"));

        verify(executorService).submit(any(Runnable.class));
    }
}