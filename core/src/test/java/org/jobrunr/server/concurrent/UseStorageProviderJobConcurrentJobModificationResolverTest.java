package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.stream.Stream;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UseStorageProviderJobConcurrentJobModificationResolverTest {

    private UseStorageProviderJobConcurrentJobModificationResolver concurrentJobModificationResolver;

    @Mock
    private JobZooKeeper jobZooKeeper;

    @BeforeEach
    void setUp() {
        concurrentJobModificationResolver = new UseStorageProviderJobConcurrentJobModificationResolver(jobZooKeeper);
    }

    @ParameterizedTest
    @MethodSource("getJobsInDifferentStates")
    void concurrentStateChangeFromSucceededFailedOrScheduledToDeletedIsAllowed(Job localJob, Job storageProviderJob) {
        final Thread jobThread = mock(Thread.class);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(jobThread);

        concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(localJob));

        verify(jobThread).interrupt();
        assertThat(localJob).hasOneOfTheFollowingStates(FAILED, SUCCEEDED);
    }

    static Stream<Arguments> getJobsInDifferentStates() {
        final Job scheduledJob = aScheduledJob().build();
        final Job jobInProgress = aJobInProgress().build();
        return Stream.of(
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withDeletedState().build()),
                // new state: scheduled
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withScheduledState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withScheduledState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withScheduledState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withScheduledState().build()),
                // new state: processing
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withProcessingState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withProcessingState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withProcessingState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withProcessingState().build()),
                // new state: failed
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withFailedState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withFailedState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withFailedState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withFailedState().build()),
                // new state: succeeded
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withSucceededState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withSucceededState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withSucceededState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withSucceededState().build())
        );
    }

}