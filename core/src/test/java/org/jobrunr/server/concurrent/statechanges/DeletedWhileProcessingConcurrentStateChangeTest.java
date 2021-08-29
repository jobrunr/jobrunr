package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeletedWhileProcessingConcurrentStateChangeTest {

    DeletedWhileProcessingConcurrentStateChange allowedStateChange;

    @Mock
    private JobZooKeeper jobZooKeeper;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new DeletedWhileProcessingConcurrentStateChange(jobZooKeeper);
    }

    @Test
    void ifJobDeletedWhileGoingToProcessing() {
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

}