package org.jobrunr.server.concurrent.statechanges;

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
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JobStateChangedWhileProcessingConcurrentStateChangeTest {

    JobStateChangedWhileProcessingConcurrentStateChange allowedStateChange;

    @Mock
    private JobZooKeeper jobZooKeeper;

    @BeforeEach
    public void setUp() {
        allowedStateChange = new JobStateChangedWhileProcessingConcurrentStateChange(jobZooKeeper);
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeAndStorageProviderJobHasNotAVersionOfPlus1ItWillNotMatch() {
        final Job localJob = aJobInProgress().withVersion(2).build();
        final Job storageProviderJob = aCopyOf(localJob).withVersion(5).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        boolean matchesAllowedStateChange = allowedStateChange.matches(localJob, storageProviderJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeAndStorageProviderJobIsAlsoProcessingItWillNotMatch() {
        final Job localJob = aJobInProgress().withVersion(2).build();
        final Job storageProviderJob = aCopyOf(localJob).withVersion(3).build();

        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(null);

        boolean matchesAllowedStateChange = allowedStateChange.matches(localJob, storageProviderJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeAndIsStillProcessingItWillNotMatch() {
        final Job localJob = aJobInProgress().withVersion(2).build();
        final Job storageProviderJob = aCopyOf(localJob).withVersion(3).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        final Thread jobThread = mock(Thread.class);
        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(jobThread);

        boolean matchesAllowedStateChange = allowedStateChange.matches(localJob, storageProviderJob);
        assertThat(matchesAllowedStateChange).isFalse();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeAndIsNotProcessingItWillMatch() {
        final Job localJob = aJobInProgress().withVersion(2).build();
        final Job storageProviderJob = aCopyOf(localJob).withVersion(3).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(null);

        boolean matchesAllowedStateChange = allowedStateChange.matches(localJob, storageProviderJob);
        assertThat(matchesAllowedStateChange).isTrue();
    }

    @Test
    void ifJobIsHavingConcurrentStateChangeWhileProcessingItWillResolveToTheStorageProviderJob() {
        final Job localJob = aJobInProgress().withVersion(2).build();
        final Job storageProviderJob = aCopyOf(localJob).withVersion(3).withState(new SucceededState(ofMillis(10), ofMillis(6))).build();

        lenient().when(jobZooKeeper.getThreadProcessingJob(localJob)).thenReturn(null);

        boolean matchesAllowedStateChange = allowedStateChange.matches(localJob, storageProviderJob);
        assertThat(matchesAllowedStateChange).isTrue();

        final ConcurrentJobModificationResolveResult resolveResult = allowedStateChange.resolve(localJob, storageProviderJob);
        assertThat(resolveResult.failed()).isFalse();
        assertThat(resolveResult.getLocalJob()).isEqualTo(storageProviderJob);
    }

}