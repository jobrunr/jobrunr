package org.jobrunr.server.concurrent;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.stream.Stream;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.JobRunrAssertions.failedJob;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.jobs.states.StateName.DELETED;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultConcurrentJobModificationResolverTest {

    private DefaultConcurrentJobModificationResolver concurrentJobModificationResolver;

    @Mock
    private StorageProvider storageProvider;
    @Mock
    private JobZooKeeper jobZooKeeper;

    @BeforeEach
    void setUp() {
        concurrentJobModificationResolver = new DefaultConcurrentJobModificationResolver(storageProvider, jobZooKeeper);
    }

    @ParameterizedTest
    @MethodSource("getJobsThatWereDeletedInDifferentStates")
    void concurrentStateChangeFromSucceededFailedOrScheduledToDeletedIsAllowed(Job localJob, Job storageProviderJob) {
        final Thread jobThread = mock(Thread.class);
        when(storageProvider.getJobById(localJob.getId())).thenReturn(storageProviderJob);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(jobThread);

        concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(localJob));

        verifyNoInteractions(jobThread);
    }

    @ParameterizedTest
    @MethodSource("getJobsThatAreInProgressInDifferentStates")
    void concurrentStateChangeWhileProcessingIsAllowedIfJobIsNotProcessingAnymore(Job localJob, Job storageProviderJob) {
        when(storageProvider.getJobById(localJob.getId())).thenReturn(storageProviderJob);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(null);

        assertThatCode(() -> concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(localJob)))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("getJobsThatAreInProgressInDifferentStates")
    void concurrentStateChangeWhileProcessingIsNotAllowedIfJobIsProcessing(Job localJob, Job storageProviderJob) {
        final Thread jobThread = mock(Thread.class);
        when(storageProvider.getJobById(localJob.getId())).thenReturn(storageProviderJob);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(jobThread);

        assertThatCode(() -> concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(localJob)))
                .isInstanceOf(UnresolvableConcurrentJobModificationException.class);
    }

    @Test
    void concurrentStateChangeFromProcessingToDeletedIsAllowedAndInterruptsThread() {
        final Job job1 = aJobInProgress().build();
        final Job job2 = aJobInProgress().build();

        final Thread job1Thread = mock(Thread.class);
        final Thread job2Thread = mock(Thread.class);

        when(storageProvider.getJobById(job1.getId())).thenReturn(aCopyOf(job1).withDeletedState().build());
        when(storageProvider.getJobById(job2.getId())).thenReturn(aCopyOf(job2).withDeletedState().build());

        when(jobZooKeeper.getThreadProcessingJob(job1)).thenReturn(job1Thread);
        when(jobZooKeeper.getThreadProcessingJob(job2)).thenReturn(job2Thread);

        concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(asList(job1, job2)));

        verify(job1Thread).interrupt();
        verify(job2Thread).interrupt();
        assertThat(job1).hasState(DELETED);
        assertThat(job2).hasState(DELETED);
    }

    @Test
    void concurrentStateChangeForJobThatIsPerformedOnOtherBackgroundJobServerIsAllowed() {
        final Job jobInProgress = aJobInProgress().build();
        Job localJob = aCopyOf(jobInProgress).withVersion(3).build();
        Job storageProviderJob = aCopyOf(jobInProgress).withVersion(6).withFailedState().withScheduledState().withEnqueuedState(now()).withProcessingState().build();

        final Thread jobThread = mock(Thread.class);
        when(storageProvider.getJobById(localJob.getId())).thenReturn(storageProviderJob);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(jobThread);

        concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(localJob));

        verify(jobThread).interrupt();
    }

    @Test
    void concurrentStateChangeFromUnsupportedStateChangeIsNotAllowedAndThrowsException() {
        final Job job1 = aJobInProgress().build();
        final Job job2 = aJobInProgress().build();

        when(storageProvider.getJobById(job1.getId())).thenReturn(aCopyOf(job1).build());
        when(storageProvider.getJobById(job2.getId())).thenReturn(aCopyOf(job2).build());

        assertThatThrownBy(() -> concurrentJobModificationResolver.resolve(new ConcurrentJobModificationException(asList(job1, job2))))
                .isInstanceOf(ConcurrentJobModificationException.class)
                .has(failedJob(job1))
                .has(failedJob(job2));
    }

    static Stream<Arguments> getJobsThatWereDeletedInDifferentStates() {
        final Job scheduledJob = aScheduledJob().build();
        final Job jobInProgress = aJobInProgress().build();
        return Stream.of(
                arguments(aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build(), aCopyOf(scheduledJob).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withSucceededState().build(), aCopyOf(jobInProgress).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withFailedState().build(), aCopyOf(jobInProgress).withDeletedState().build()),
                arguments(aCopyOf(jobInProgress).withScheduledState().build(), aCopyOf(jobInProgress).withDeletedState().build())
        );
    }

    static Stream<Arguments> getJobsThatAreInProgressInDifferentStates() {
        final Job jobInProgress = aJobInProgress().withVersion(2).build();
        return Stream.of(
                arguments(jobInProgress, aCopyOf(jobInProgress).withVersion(3).withSucceededState().build()),
                arguments(jobInProgress, aCopyOf(jobInProgress).withVersion(3).withFailedState().build()),
                arguments(jobInProgress, aCopyOf(jobInProgress).withVersion(3).withScheduledState().build())
        );
    }

}