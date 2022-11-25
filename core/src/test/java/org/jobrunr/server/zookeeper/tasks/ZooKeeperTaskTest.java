package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.server.zookeeper.ZooKeeperRunTaskInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import static java.time.Instant.now;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.mockito.Mockito.verifyNoInteractions;

public class ZooKeeperTaskTest extends AbstractZooKeeperTaskTest {

    ProcessRecurringJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessRecurringJobsTask(jobZooKeeper, backgroundJobServer);
    }

    @Test
    void masterTasksArePostponedToNextRunIfPollIntervalInSecondsTimeboxIsAboutToPass() {
        ZooKeeperRunTaskInfo zooKeeperRunTaskInfo = zooKeeperStatistics.startRun(aDefaultBackgroundJobServerStatus().build());
        setRunStartTimeInPast(zooKeeperRunTaskInfo, 15);

        task.run(zooKeeperRunTaskInfo);

        verifyNoInteractions(storageProvider);
    }

    private static void setRunStartTimeInPast(ZooKeeperRunTaskInfo zooKeeperRunTaskInfo, int secondsInPast) {
        Whitebox.setInternalState(zooKeeperRunTaskInfo, "runStartTime", now().minusSeconds(secondsInPast));
    }
}
