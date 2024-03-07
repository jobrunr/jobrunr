package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.tasks.AbstractZooKeeperTaskTest;
import org.jobrunr.server.tasks.PeriodicTaskRunInfo;
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
        task = new ProcessRecurringJobsTask(backgroundJobServer);
    }

    @Test
    void masterTasksArePostponedToNextRunIfPollIntervalInSecondsTimeboxIsAboutToPass() {
        PeriodicTaskRunInfo periodicTaskRunInfo = zooKeeperStatistics.startRun(backgroundJobServer.getConfiguration());
        setRunStartTimeInPast(periodicTaskRunInfo, 15);

        task.run(periodicTaskRunInfo);

        verifyNoInteractions(storageProvider);
    }

    @Test
    void ifProcessToJobListHasNullItIsNotSaved() {
        // GIVEN
        List<Object> items = Arrays.asList(null, null);
        Function<Object, Job> toJobFunction = x -> (Job) x;

        // WHEN
        //task.convertAndProcessJobs(items, toJobFunction);

        // THEN
        verify(storageProvider, never()).save(anyList());

        throw new UnsupportedOperationException("test me");
    }

    private static void setRunStartTimeInPast(PeriodicTaskRunInfo zooKeeperRunTaskInfo, int secondsInPast) {
        Whitebox.setInternalState(zooKeeperRunTaskInfo, "runStartTime", now().minusSeconds(secondsInPast));
    }
}
