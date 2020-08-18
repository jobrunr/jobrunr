package org.jobrunr.server.strategy;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.JobZooKeeper;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasicWorkDistributionStrategyTest {


    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private BackgroundJobServerStatus backgroundJobServerStatus;
    @Mock
    private JobZooKeeper jobZooKeeper;
    private BasicWorkDistributionStrategy workDistributionStrategy;

    @BeforeEach
    void setUpWorkDistributionStrategy() {
        when(backgroundJobServer.getServerStatus()).thenReturn(backgroundJobServerStatus);
        workDistributionStrategy = new BasicWorkDistributionStrategy(backgroundJobServer, jobZooKeeper);
    }

    @Test
    void canOnboardIfWorkQueueSizeIsEmpty() {
        when(backgroundJobServerStatus.getWorkerPoolSize()).thenReturn(100);
        when(jobZooKeeper.getOccupiedWorkerCount()).thenReturn(0);

        assertThat(workDistributionStrategy.canOnboardNewWork()).isTrue();
    }

    @Test
    void canNotOnboardIfWorkQueueIsFull() {
        when(backgroundJobServerStatus.getWorkerPoolSize()).thenReturn(100);
        when(jobZooKeeper.getOccupiedWorkerCount()).thenReturn(100);

        assertThat(workDistributionStrategy.canOnboardNewWork()).isFalse();
    }

    @Test
    void canOnboardIfMoreThan30PercentFreeInWorkQueue() {
        when(backgroundJobServerStatus.getWorkerPoolSize()).thenReturn(100);
        when(jobZooKeeper.getOccupiedWorkerCount()).thenReturn(69);

        assertThat(workDistributionStrategy.canOnboardNewWork()).isTrue();
    }

    @Test
    void canNotOnboardIfLessThan30PercentFreeInWorkQueue() {
        when(backgroundJobServerStatus.getWorkerPoolSize()).thenReturn(100);
        when(jobZooKeeper.getOccupiedWorkerCount()).thenReturn(71);

        assertThat(workDistributionStrategy.canOnboardNewWork()).isFalse();
    }

}