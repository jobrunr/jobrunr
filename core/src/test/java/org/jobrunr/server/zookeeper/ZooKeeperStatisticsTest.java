package org.jobrunr.server.zookeeper;


import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ZooKeeperStatisticsTest {

    ZooKeeperStatistics statistics;
    @Mock
    DashboardNotificationManager dashboardNotificationManager;

    @BeforeEach
    void setUp() {
        statistics = new ZooKeeperStatistics(dashboardNotificationManager);
    }

    @Test
    void ifOneRunTookTooLongNoNotificationIsShown() {
        statistics.logRun(2, true, 5, now().minusSeconds(15), now());

        verify(dashboardNotificationManager, never()).notify(any(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class));
    }

    @Test
    void ifThreeConsecutiveRunsTookTooLongANotificationIsShown() {
        statistics.logRun(2, true, 5, now().minusSeconds(45), now().minusSeconds(30));
        statistics.logRun(3, true, 5, now().minusSeconds(30), now().minusSeconds(15));
        statistics.logRun(4, true, 5, now().minusSeconds(15), now());

        verify(dashboardNotificationManager).notify(any(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class));
    }

    @Test
    void ifThreeNotConsecutiveRunsTookTooLongNoNotificationIsShown() {
        statistics.logRun(2, true, 5, now().minusSeconds(45), now().minusSeconds(30));
        statistics.logRun(3, true, 5, now().minusSeconds(30), now().minusSeconds(15));
        statistics.logRun(4, true, 5, now().minusSeconds(15), now().minusSeconds(11));

        verify(dashboardNotificationManager, never()).notify(any(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class));
    }

    @Test
    void ifRunFailsMoreThan5Times_hasTooManyExceptionsIsTrue() {
        statistics.handleException(new RuntimeException("Ex 1"));
        statistics.handleException(new RuntimeException("Ex 2"));
        statistics.handleException(new RuntimeException("Ex 3"));
        statistics.handleException(new RuntimeException("Ex 4"));
        statistics.handleException(new RuntimeException("Ex 5"));
        statistics.handleException(new RuntimeException("Ex 5"));

        assertThat(statistics.hasTooManyExceptions()).isTrue();
    }

    @Test
    void ifRunFailsOnceThenSucceedsAgainexceptionCounterIsZero() {
        statistics.handleException(new RuntimeException("Ex 1"));

        statistics.logRun(1, true, 5, now().minusSeconds(1), now());

        assertThat(statistics).hasFieldOrPropertyWithValue("exceptionCounter", 0);
    }

    @Test
    void ifRunFails3TimesThenSucceedsMultipleTimesAgainexceptionCounterIsZero() {
        statistics.handleException(new RuntimeException("Ex 1"));
        statistics.handleException(new RuntimeException("Ex 2"));
        statistics.handleException(new RuntimeException("Ex 3"));

        statistics.logRun(1, true, 5, now().minusSeconds(1), now());
        statistics.logRun(1, true, 5, now().minusSeconds(1), now());
        statistics.logRun(1, true, 5, now().minusSeconds(1), now());
        statistics.logRun(1, true, 5, now().minusSeconds(1), now());
        statistics.logRun(1, true, 5, now().minusSeconds(1), now());

        assertThat(statistics).hasFieldOrPropertyWithValue("exceptionCounter", 0);
    }
}