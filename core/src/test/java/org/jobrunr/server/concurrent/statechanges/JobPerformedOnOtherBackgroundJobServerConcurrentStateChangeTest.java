package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.*;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JobPerformedOnOtherBackgroundJobServerConcurrentStateChangeTest {

    @Mock
    JobZooKeeper jobZooKeeper;

    @Mock
    Thread threadProcessingLocalJob;

    JobPerformedOnOtherBackgroundJobServerConcurrentStateChange allowedStateChange;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new JobPerformedOnOtherBackgroundJobServerConcurrentStateChange(jobZooKeeper);
        lenient().when(jobZooKeeper.getThreadProcessingJob(any())).thenReturn(threadProcessingLocalJob);
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
                .withState(new ProcessingState(UUID.randomUUID()))
                .build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(jobInProgress, jobInProgressOnOtherServer);
        assertThat(matchesAllowedStateChange).isTrue();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeOnDifferentServerItWillResolveToTheStorageProviderJob() {
        //GIVEN
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressOnOtherServer = aCopyOf(jobInProgress)
                .withState(new FailedState("Orphaned job", new IllegalStateException("Not important")))
                .withState(new ScheduledState(Instant.now()))
                .withState(new EnqueuedState())
                .withState(new ProcessingState(UUID.randomUUID()))
                .build();

        // WHEN
        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgress, jobInProgressOnOtherServer);

        // THEN
        assertThat(resolveResult.failed()).isFalse();
        assertThat(resolveResult.getLocalJob()).isEqualTo(jobInProgressOnOtherServer);
        verify(threadProcessingLocalJob).interrupt();
    }

}