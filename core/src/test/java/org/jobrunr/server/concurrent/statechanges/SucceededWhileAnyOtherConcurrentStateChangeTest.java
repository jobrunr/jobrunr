package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobSteward;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SucceededWhileAnyOtherConcurrentStateChangeTest {

    SucceededWhileAnyOtherConcurrentStateChange allowedStateChange;

    @Mock
    private JobSteward jobZooKeeper;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new SucceededWhileAnyOtherConcurrentStateChange(jobZooKeeper);
    }

    @Test
    void ifJobSucceededWhileInProgress() {
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressWithUpdate = aCopyOf(jobInProgress).withMetadata("extra", "metadata").build();
        final Job succeededJob = aCopyOf(jobInProgress).withSucceededState().build();

        Thread mockThread = mock(Thread.class);
        when(jobZooKeeper.getThreadProcessingJob(jobInProgressWithUpdate)).thenReturn(mockThread);

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgressWithUpdate, succeededJob);
        assertThat(resolveResult.failed()).isFalse();
        verify(mockThread).interrupt();
    }

    @Test
    void ifJobSucceededWhileGoingToProcessingButThreadIsAlreadyRemoved() {
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressWithUpdate = aCopyOf(jobInProgress).withMetadata("extra", "metadata").build();
        final Job succeededJob = aCopyOf(jobInProgress).withSucceededState().build();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgressWithUpdate, succeededJob);
        assertThat(resolveResult.failed()).isFalse();
    }

    @Test
    void ifJobEnqueuedAgainWhileBeingSucceededAlreadyThisCanBeResolved() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final Job succeededJob = aSucceededJob().build();

        assertThat(allowedStateChange.matches(enqueuedJob, succeededJob)).isTrue();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(enqueuedJob, succeededJob);
        assertThat(resolveResult.failed()).isFalse();

        verifyNoInteractions(jobZooKeeper);
    }

    @Test
    void ifJobScheduledAgainWhileBeingSucceededAlreadyThisCanBeResolved() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final Job succeededJob = aSucceededJob().build();

        assertThat(allowedStateChange.matches(enqueuedJob, succeededJob)).isTrue();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(enqueuedJob, succeededJob);
        assertThat(resolveResult.failed()).isFalse();

        verifyNoInteractions(jobZooKeeper);
    }

    @Test
    void ifJobSucceededAgainWhileBeingSucceededThisCanNOTBeResolvedAsSomethingIsWrongThen() {
        final Job jobInProgress = aJobInProgress().build();
        final Job storageSucceededJob = aCopyOf(jobInProgress).withSucceededState().build();
        final Job localSucceededJob = aCopyOf(jobInProgress).withSucceededState().build();

        assertThat(allowedStateChange.matches(localSucceededJob, storageSucceededJob)).isFalse();

        assertThatCode(() -> allowedStateChange.resolve(localSucceededJob, storageSucceededJob))
                .isInstanceOf(JobRunrException.class);
    }

}