package org.jobrunr.server.concurrent.statechanges;

import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.concurrent.ConcurrentJobModificationResolveResult;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledTooEarlyByJobZooKeeperConcurrentStateChangeTest {

    @Mock
    StorageProvider storageProvider;

    @Captor
    ArgumentCaptor<Job> jobCaptor;

    ScheduledTooEarlyByJobZooKeeperConcurrentStateChange allowedStateChange;


    @BeforeEach
    public void setUp() {
        allowedStateChange = new ScheduledTooEarlyByJobZooKeeperConcurrentStateChange(storageProvider);
    }

    @Test
    void ifLocalJobHasOtherThanEnqueuedStateItWillNotMatch() {
        final Job scheduledJob = aScheduledJob().build();
        final Job enqueuedJob = aCopyOf(scheduledJob).withEnqueuedState(Instant.now()).build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(scheduledJob, enqueuedJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifStorageJobHasOtherThanScheduledStateItWillNotMatch() {
        final Job enqueuedJob = anEnqueuedJob().build();
        final Job succeededJob = aCopyOf(enqueuedJob).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(enqueuedJob, succeededJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobHasEnqueuedStateAndWasScheduledNormallyItWillNotMatch() {
        final Job jobInProgress = aJobInProgress().build();
        final Job succeededJob = aCopyOf(jobInProgress).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(jobInProgress, succeededJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobHasEnqueuedStateAndWasScheduledTooEarlyByJobZooKeeperItWillMatch() {
        final Job scheduledJob = aJob()
                .withFailedState()
                .withScheduledState()
                .withVersion(5)
                .build();
        final Job enqueuedJob = aCopyOf(scheduledJob)
                .withEnqueuedState(Instant.now())
                .withVersion(4)
                .build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(enqueuedJob, scheduledJob);
        assertThat(matchesAllowedStateChange).isTrue();
    }

    @Test
    void ifJobHasEnqueuedStateAndWasScheduledTooEarlyByJobZooKeeperItWillResolveLocalJobAndSaveIt() {
        final Job scheduledJob = aJob()
                .withFailedState()
                .withScheduledState()
                .withVersion(5)
                .build();
        final Job enqueuedJob = aCopyOf(scheduledJob)
                .withEnqueuedState(Instant.now())
                .withVersion(4)
                .build();


        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(enqueuedJob, scheduledJob);

        verify(storageProvider).save(jobCaptor.capture());

        assertThat(jobCaptor.getValue())
                .hasVersion(5)
                .hasStates(StateName.FAILED, StateName.SCHEDULED, StateName.ENQUEUED);
        assertThat(resolveResult.failed()).isFalse();
        assertThat(resolveResult.getLocalJob()).isEqualTo(enqueuedJob);
    }
}