package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.server.tasks.OneOffTaskRunInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class AbstractJobZooKeeperTaskTest extends AbstractTaskTest {

    AbstractJobZooKeeperTask task;

    @BeforeEach
    void setUpTask() {
        task = new AbstractJobZooKeeperTask(backgroundJobServer) {
            @Override
            protected void runTask() {
                // nothing to do
            }
        };
        Whitebox.setInternalState(task, "runInfo", new OneOffTaskRunInfo(backgroundJobServer.getConfiguration()));
    }

    @Test
    void ifProcessToJobListHasNullItIsNotSaved() {
        // GIVEN
        List<Object> items = asList(null, null);
        Function<Object, List<Job>> toJobFunction = x -> asList(null, null);

        // WHEN
        task.convertAndProcessManyJobs(items, toJobFunction, System.out::println);

        // THEN
        verify(storageProvider, never()).save(anyList());
    }

    @Test
    void convertAndProcessManyJobsReturnsPreviousResultsSoSupplierCanChooseToContinueOrNot() {
        // GIVEN
        Function<List<Job>, List<Job>> itemSupplier = jobs -> {
            if(jobs == null) return asList(anEnqueuedJob().build(), anEnqueuedJob().build());
            else if(jobs.size() != 2) throw new IllegalStateException("Previous list with size 2 was expected");
            else return emptyList();
        };
        Function<Job, Job> toJobFunction = job -> job;

        // WHEN & THEN
        assertThatCode(() -> task.convertAndProcessManyJobs(itemSupplier, toJobFunction, System.out::println))
                .doesNotThrowAnyException();
    }
}
