package org.jobrunr.jobs.states;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aJob;

class CarbonAwareAwaitingStateTest {

    @Test
    void testCarbonAwareJobsOnCreationThrowsExceptionIfDeadlineNot3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(2, HOURS))).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Carbon Aware jobs should have a deadline at least 3 hours in the future");
    }

    @Test
    void testCarbonAwareJobsOnCreationAcceptsDeadlineIfEqualTo3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(3, HOURS))).build())
                .doesNotThrowAnyException();
    }

    @Test
    void testCarbonAwareJobsOnCreationAcceptsDeadlineIfMoreThan3HoursInTheFuture() {
        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(4, HOURS))).build())
                .doesNotThrowAnyException();

        assertThatCode(() -> aJob().withState(new CarbonAwareAwaitingState(now().plus(1, MONTHS))).build())
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
    void testMoveToNextStateValidatesIdealMomentIsBeforeDeadline() {
        // GIVEN
        Instant deadline = now().plus(1, DAYS);
        CarbonAwareAwaitingState state = new CarbonAwareAwaitingState(deadline);
        Job carbonAwareJob = aJob().withState(state).build();

        // WHEN
        Instant idealMoment = now().plus(2, DAYS);
        state.moveToNextState(carbonAwareJob, idealMoment);

        // THEN
        assertThat(carbonAwareJob).hasState(StateName.SCHEDULED);
        ScheduledState scheduledState = carbonAwareJob.getJobState();
        assertThat(scheduledState.getScheduledAt()).isEqualTo(deadline);
        assertThat(scheduledState.getReason()).isEqualTo("Job scheduled at " + deadline + " as ideal moment surpassed the deadline.");
    }
}