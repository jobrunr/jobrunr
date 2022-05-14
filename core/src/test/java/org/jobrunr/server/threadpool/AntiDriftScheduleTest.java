package org.jobrunr.server.threadpool;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AntiDriftScheduleTest {

    Runnable runnable = () -> System.out.println("Some runnable");

    @Test
    void antiDriftScheduleReturnsCorrectSchedule() {
        AntiDriftSchedule antiDriftSchedule = new AntiDriftSchedule(runnable, Duration.ZERO, Duration.ofSeconds(15));

        Instant scheduledAt = antiDriftSchedule.getScheduledAt();
        assertThat(scheduledAt).isCloseTo(Instant.now(), within(100, ChronoUnit.MILLIS));

        assertThat(antiDriftSchedule.getNextSchedule()).isEqualTo(scheduledAt);
        assertThat(antiDriftSchedule.getNextSchedule()).isEqualTo(scheduledAt.plusSeconds(15));
        assertThat(antiDriftSchedule.getNextSchedule()).isEqualTo(scheduledAt.plusSeconds(30));
        assertThat(antiDriftSchedule.getNextSchedule()).isEqualTo(scheduledAt.plusSeconds(45));
        assertThat(antiDriftSchedule.getNextSchedule()).isEqualTo(scheduledAt.plusSeconds(60));
    }
}