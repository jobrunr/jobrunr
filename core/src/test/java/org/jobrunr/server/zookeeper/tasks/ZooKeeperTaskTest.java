package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.zookeeper.ZooKeeperRunTaskInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.time.Instant.now;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ZooKeeperTaskTest extends AbstractZooKeeperTaskTest {

    ProcessRecurringJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new ProcessRecurringJobsTask(jobZooKeeper, backgroundJobServer);
    }

    @Test
    void masterTasksArePostponedToNextRunIfPollIntervalInSecondsTimeboxIsAboutToPass() {
        ZooKeeperRunTaskInfo zooKeeperRunTaskInfo = zooKeeperStatistics.startRun(backgroundJobServer.getConfiguration());
        setRunStartTimeInPast(zooKeeperRunTaskInfo, 15);

        task.run(zooKeeperRunTaskInfo);

        verifyNoInteractions(storageProvider);
    }

    @Test
    void ifProcessToJobListHasNullItIsNotSaved() {
        // GIVEN
        List<Object> items = Arrays.asList(null, null);
        Function<Object, Job> toJobFunction = x -> (Job)x;

        // WHEN
        task.convertAndProcessJobs(items, toJobFunction);

        // THEN
        verify(storageProvider, never()).save(anyList());
    }

    private static void setRunStartTimeInPast(ZooKeeperRunTaskInfo zooKeeperRunTaskInfo, int secondsInPast) {
        Whitebox.setInternalState(zooKeeperRunTaskInfo, "runStartTime", now().minusSeconds(secondsInPast));
    }
}
