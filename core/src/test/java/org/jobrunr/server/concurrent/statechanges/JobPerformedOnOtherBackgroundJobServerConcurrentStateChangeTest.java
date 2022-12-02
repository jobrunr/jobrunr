package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.*;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;

class JobPerformedOnOtherBackgroundJobServerConcurrentStateChangeTest {

    JobPerformedOnOtherBackgroundJobServerConcurrentStateChange allowedStateChange;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new JobPerformedOnOtherBackgroundJobServerConcurrentStateChange();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeOnSameServerItWillNotMatch() {
        final Job jobInProgress = aJobInProgress().build();
        final Job succeededJob = aCopyOf(jobInProgress).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(jobInProgress, succeededJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeOnDifferentServerItWillMatch() {
        final Job jobInProgress = aJobInProgress()
                .withVersion(3)
                .build();
        final Job jobInProgressOnOtherServer = aCopyOf(jobInProgress)
                .withVersion(6)
                .withState(new FailedState("Orphaned job", new IllegalStateException("Not important")))
                .withState(new ScheduledState(Instant.now()))
                .withState(new EnqueuedState())
                .withState(new ProcessingState(UUID.randomUUID(), "Not important"))
                .build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(jobInProgress, jobInProgressOnOtherServer);
        assertThat(matchesAllowedStateChange).isTrue();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeOnDifferentServerItWillResolveToTheStorageProviderJob() {
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressOnOtherServer = aCopyOf(jobInProgress)
                .withState(new FailedState("Orphaned job", new IllegalStateException("Not important")))
                .withState(new ScheduledState(Instant.now()))
                .withState(new EnqueuedState())
                .withState(new ProcessingState(UUID.randomUUID(), "Not important"))
                .build();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgress, jobInProgressOnOtherServer);
        assertThat(resolveResult.failed()).isFalse();
        assertThat(resolveResult.getLocalJob()).isEqualTo(jobInProgressOnOtherServer);
    }

}