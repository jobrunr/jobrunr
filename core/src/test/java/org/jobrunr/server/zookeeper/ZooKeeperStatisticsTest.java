package org.jobrunr.server.zookeeper;


import org.jobrunr.server.dashboard.DashboardNotificationManager;
import org.jobrunr.server.dashboard.PollIntervalInSecondsTimeBoxIsTooSmallNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
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
    void ifRunTookTooLongANotificationIsShown() {
        statistics.logRun(2, 5, Instant.now().minusSeconds(15), Instant.now());

        verify(dashboardNotificationManager).notify(any(PollIntervalInSecondsTimeBoxIsTooSmallNotification.class));
    }
}