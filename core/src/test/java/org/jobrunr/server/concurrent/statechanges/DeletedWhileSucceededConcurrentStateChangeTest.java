package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class DeletedWhileSucceededConcurrentStateChangeTest {

    DeletedWhileSucceededConcurrentStateChange allowedStateChange;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new DeletedWhileSucceededConcurrentStateChange();
    }

    @Test
    void ifJobDeletedWhileGoingToSucceededStateThereIsNoInterruptNeeded() {
        final Job jobInProgress = aJobInProgress().build();
        final Job succeededJob = aCopyOf(jobInProgress).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();
        final Job deletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(succeededJob, deletedJob);
        assertThat(resolveResult.failed()).isFalse();
    }

}