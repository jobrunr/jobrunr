package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import static java.time.Duration.ZERO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AntiDriftSchedulerTest {

    @Mock
    JobRunrExecutor jobRunrExecutor;
    @Mock
    ScheduledFuture scheduledFuture;

    @Captor
    ArgumentCaptor<Duration> durationArgumentCaptor;

    AntiDriftScheduler antiDriftScheduler;

    @BeforeEach
    void setUp() {
        antiDriftScheduler = new AntiDriftScheduler(jobRunrExecutor);
    }

    @Test
    void antiDriftSchedulerWhenNoSchedulesDoesNothing() {
        antiDriftScheduler.run();

        verifyNoInteractions(jobRunrExecutor);
    }

    @Test
    void antiDriftSchedulerSchedulesRunnables() {
        // GIVEN
        Runnable runnable = () -> System.out.println("The runnable");
        AntiDriftSchedule antiDriftSchedule = new AntiDriftSchedule(runnable, ZERO, Duration.ofMillis(1000));
        antiDriftScheduler.addSchedule(antiDriftSchedule);
        when(jobRunrExecutor.schedule(any(), any())).thenReturn(scheduledFuture);

        // WHEN
        antiDriftScheduler.run();

        // THEN
        verify(jobRunrExecutor).schedule(eq(runnable), durationArgumentCaptor.capture());
        assertThat(durationArgumentCaptor.getValue()).isCloseTo(ZERO, Duration.ofMillis(250));
        reset(jobRunrExecutor);
        when(jobRunrExecutor.schedule(any(), any())).thenReturn(scheduledFuture);

        // WHEN
        antiDriftScheduler.run();

        // THEN
        verify(jobRunrExecutor).schedule(eq(runnable), durationArgumentCaptor.capture());
        assertThat(durationArgumentCaptor.getValue()).isCloseTo(Duration.ofMillis(850), Duration.ofMillis(250));
        reset(jobRunrExecutor);

        // WHEN
        antiDriftScheduler.run();

        // THEN
        verifyNoInteractions(jobRunrExecutor);
    }

    @Test
    void onStopAntiDriftSchedulerCancelsExistingTasks() {
        // GIVEN
        Runnable runnable = () -> System.out.println("The runnable");
        AntiDriftSchedule antiDriftSchedule = new AntiDriftSchedule(runnable, ZERO, Duration.ofMillis(1000));
        antiDriftScheduler.addSchedule(antiDriftSchedule);
        when(jobRunrExecutor.schedule(any(), any())).thenReturn(scheduledFuture);
        antiDriftScheduler.run();

        // WHEN
        antiDriftScheduler.stop();

        // THEN
        verify(scheduledFuture).cancel(false);

    }

}