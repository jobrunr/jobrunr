package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;

class CarbonAwareAwaitingStateTest {

    @Test
    void testCarbonAwareJobsOnCreationThrowsExceptionIfDeadlineNot3HoursInTheFuture() {
        Instant now = now();
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now.plus(2, HOURS))).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(String.format("'to' (=%s) must be at least 3 hours in the future to use carbon aware scheduling", now.plus(2, HOURS)));
    }

    @Test
    void testCarbonAwareJobsOnCreationAcceptsDeadlineIfEqualTo3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(3, HOURS).plusMillis(1000))).build())
                .doesNotThrowAnyException();
    }

    @Test
    void testCarbonAwareJobsOnCreationAcceptsDeadlineIfMoreThan3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(4, HOURS))).build())
                .doesNotThrowAnyException();

        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(1, DAYS))).build())
                .doesNotThrowAnyException();
    }

    @Test
    void testMoveToNextStateMovesJobToScheduledStateAtIdealMoment() {
        // GIVEN
        CarbonAwareAwaitingState state = new CarbonAwareAwaitingState(now().plus(1, DAYS));
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
                .hasMessage("Only jobs in AWAITING can move to a next state");
    }
}