package org.jobrunr.server.threadpool;


import org.jobrunr.server.JobSteward;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobRunrExecutorTest {

    @Mock
    private JobSteward jobSteward;

    @Spy
    private JobRunrExecutor jobRunrExecutor = new VirtualThreadJobRunrExecutor(10, "test");

    @Captor
    private ArgumentCaptor<Duration> executorStopDurationCaptor;

    @Test
    void stopShouldSplitTotalInterruptDurationAndPollJobStewardUntilAllWorkersAreIdle() {
        // GIVEN
        Duration totalDuration = Duration.ofMillis(1000);
        when(jobSteward.getOccupiedWorkerCount())
                .thenReturn(2) // First check
                .thenReturn(0); // Second check

        // WHEN
        jobRunrExecutor.stop(jobSteward, totalDuration);

        // THEN
        verify(jobRunrExecutor).stop(executorStopDurationCaptor.capture());
        assertThat(executorStopDurationCaptor.getValue()).isEqualTo(Duration.ofMillis(750));

        verify(jobSteward, atLeast(2)).getOccupiedWorkerCount();
    }

    @Test
    void stopShouldTimeoutWhenWorkersDoNotFinishInTime() {
        // GIVEN
        Duration totalDuration = Duration.ofMillis(50);
        when(jobSteward.getOccupiedWorkerCount()).thenReturn(5);

        // WHEN
        long startTime = System.currentTimeMillis();
        jobRunrExecutor.stop(jobSteward, totalDuration);
        long endTime = System.currentTimeMillis();

        // THEN
        assertThat(endTime - startTime).isLessThan(300);
        verify(jobRunrExecutor).stop(executorStopDurationCaptor.capture());
        assertThat(executorStopDurationCaptor.getValue()).isEqualTo(Duration.ZERO);
    }

    @Test
    void stopShouldHandleInterruption() {
        // GIVEN
        when(jobSteward.getOccupiedWorkerCount()).thenReturn(5);
        // Interrupt the thread immediately, will cause InterruptedException on Thread.sleep
        Thread.currentThread().interrupt();

        // WHEN
        jobRunrExecutor.stop(jobSteward, Duration.ofMillis(1000));

        // THEN
        assertThat(Thread.interrupted()).isTrue();
        verify(jobRunrExecutor).stop(any(Duration.class));
    }

}