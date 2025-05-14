package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.scheduling.CarbonAwareScheduleMargin;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.RecurringJobTestBuilder.aDefaultRecurringJob;
import static org.jobrunr.scheduling.CarbonAwareScheduleMargin.margin;

class CarbonAwareAwaitingStateTest {

    @Test
    void creatingAwaitingState_InvalidPeriods_ThrowsException() {
        assertThatCode(() -> new CarbonAwareAwaitingState(Instant.now().plusSeconds(12), Instant.now())).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new CarbonAwareAwaitingState(null, Instant.now())).isInstanceOf(IllegalArgumentException.class);
        assertThatCode(() -> new CarbonAwareAwaitingState(Instant.now(), null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void carbonAwareJobsOnCreationAcceptsDeadlineIfEqualTo3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now(), now().plus(3, HOURS).plusMillis(1000))).build())
                .doesNotThrowAnyException();
    }

    @Test
    void carbonAwareJobsOnCreationAcceptsDeadlineIfMoreThan3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now(), now().plus(4, HOURS))).build())
                .doesNotThrowAnyException();

        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now(), now().plus(1, DAYS))).build())
                .doesNotThrowAnyException();
    }

    @Test
    void moveToNextStateMovesJobToScheduledStateAtIdealMoment() {
        // GIVEN
        CarbonAwareAwaitingState state = new CarbonAwareAwaitingState(now(), now().plus(1, DAYS));
        Job carbonAwareJob = aJob().withState(state).build();

        // WHEN
        Instant idealMoment = now().plus(14, HOURS);
        state.moveToNextState(carbonAwareJob, idealMoment);

        // THEN
        assertThat(carbonAwareJob).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = carbonAwareJob.getJobState();
        assertThat(scheduledState.getScheduledAt()).isEqualTo(idealMoment);
        assertThat(scheduledState.getReason()).isEqualTo("Job scheduled at " + idealMoment + " to minimize carbon impact.");
    }

    @Test
    void moveToNextStateWithInvalidJobStateShouldThrowException() {
        Instant deadline = Instant.now().plus(5, HOURS);
        Instant idealMoment = deadline.minus(1, HOURS);
        CarbonAwareAwaitingState carbonAwareAwaitingState = new CarbonAwareAwaitingState();
        Job notCarbonAwareJob = aJob().withState(new ScheduledState()).build();

        assertThatCode(() -> carbonAwareAwaitingState.moveToNextState(notCarbonAwareJob, idealMoment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only jobs in CarbonAwaitingState can move to a next state");
    }

    @Test
    void toCarbonAwareAwaitingState() {
        CarbonAwareScheduleMargin carbonAwareScheduleMargin = margin(Duration.ofHours(2), Duration.ofHours(10));

        Instant now = Instant.now();
        RecurringJob recurringJob = aDefaultRecurringJob().withId("123").withName("my recurring job").build();
        assertThat(CarbonAwareAwaitingState.fromRecurringJob(carbonAwareScheduleMargin, now, recurringJob))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CarbonAwareAwaitingState(now, now.minus(Duration.ofHours(2)), now.plus(Duration.ofHours(10)), "Awaiting by recurring job 'my recurring job'"));

        Instant twoDaysLater = Instant.now().plus(2, ChronoUnit.DAYS);
        assertThat(CarbonAwareAwaitingState.fromRecurringJob(carbonAwareScheduleMargin, twoDaysLater, recurringJob))
                .usingRecursiveComparison()
                .ignoringFields("createdAt")
                .isEqualTo(new CarbonAwareAwaitingState(twoDaysLater, twoDaysLater.minus(Duration.ofHours(2)), twoDaysLater.plus(Duration.ofHours(10)), "Awaiting by recurring job 'my recurring job'"));
    }
}