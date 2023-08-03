package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletedWhileAnyOtherConcurrentStateChangeTest {

    DeletedWhileAnyOtherConcurrentStateChange allowedStateChange;

    @Mock
    private JobZooKeeper jobZooKeeper;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new DeletedWhileAnyOtherConcurrentStateChange(jobZooKeeper);
    }

    @Test
    void ifJobDeletedWhileInProgress() {
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressWithUpdate = aCopyOf(jobInProgress).withMetadata("extra", "metadata").build();
        final Job deletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        Thread mockThread = mock(Thread.class);
        when(jobZooKeeper.getThreadProcessingJob(jobInProgressWithUpdate)).thenReturn(mockThread);

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgressWithUpdate, deletedJob);
        assertThat(resolveResult.failed()).isFalse();
        verify(mockThread).interrupt();
    }

    @Test
    void ifJobDeletedWhileGoingToProcessingButThreadIsAlreadyRemoved() {
        final Job jobInProgress = aJobInProgress().build();
        final Job jobInProgressWithUpdate = aCopyOf(jobInProgress).withMetadata("extra", "metadata").build();
        final Job deletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(jobInProgressWithUpdate, deletedJob);
        assertThat(resolveResult.failed()).isFalse();
    }

    @Test
    void ifJobDeletedWhileGoingToSucceededStateThereIsNoInterruptNeeded() {
        final Job jobInProgress = aJobInProgress().build();
        final Job succeededJob = aCopyOf(jobInProgress).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();
        final Job deletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        assertThat(allowedStateChange.matches(succeededJob, deletedJob)).isTrue();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(succeededJob, deletedJob);
        assertThat(resolveResult.failed()).isFalse();

        verifyNoInteractions(jobZooKeeper);
    }

    @Test
    void ifJobDeletedWhileBeingEnqueuedAgainThisCanBeResolved() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final Job deletedJob = aCopyOf(enqueuedJob).withDeletedState().build();

        assertThat(allowedStateChange.matches(enqueuedJob, deletedJob)).isTrue();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(enqueuedJob, deletedJob);
        assertThat(resolveResult.failed()).isFalse();

        verifyNoInteractions(jobZooKeeper);
    }

    @Test
    void ifJobDeletedAgainWhileBeingDeletedThisCanNOTBeResolvedAsSomethingIsWrongThen() {
        final Job jobInProgress = aJobInProgress().build();
        final Job storageDeletedJob = aCopyOf(jobInProgress).withDeletedState().build();
        final Job localDeletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        assertThat(allowedStateChange.matches(localDeletedJob, storageDeletedJob)).isFalse();

        assertThatCode(() -> allowedStateChange.resolve(localDeletedJob, storageDeletedJob))
                .isInstanceOf(JobRunrException.class);
    }

}