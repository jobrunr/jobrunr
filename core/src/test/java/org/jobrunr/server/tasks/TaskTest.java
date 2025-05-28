package org.jobrunr.server.tasks;

import org.jobrunr.SevereJobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.server.concurrent.UnresolvableConcurrentJobModificationException;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.List;
import java.util.function.Function;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.jobrunr.jobs.JobTestBuilder.aCopyOf;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TaskTest extends AbstractTaskTest {

    Task task;

    @BeforeEach
    void setUpTask() {
        task = new Task(backgroundJobServer) {
            @Override
            protected void runTask() {
                // nothing to do
            }
        };
    }

    @Test
    void onConcurrentJobModificationExceptionTaskTriesToResolveAndThrowsNoExceptionIfResolved() {
        Job jobInProgress = aJobInProgress().build();
        Job deletedJob = aCopyOf(jobInProgress).withDeletedState().build();

        doThrow(new ConcurrentJobModificationException(jobInProgress)).when(storageProvider).save(anyList());
        doReturn(deletedJob).when(storageProvider).getJobById(jobInProgress.getId());

        assertThatCode(() -> task.saveAndRunJobFilters(singletonList(jobInProgress))).doesNotThrowAnyException();
    }

    @Test
    void onConcurrentJobModificationExceptionTaskTriesToResolveAndThrowsExceptionIfNotResolved() {
        Job jobInProgress = aJobInProgress().build();
        Job enqueuedJob = aCopyOf(jobInProgress).withEnqueuedState(now()).build();

        doThrow(new ConcurrentJobModificationException(jobInProgress)).when(storageProvider).save(anyList());
        doReturn(enqueuedJob).when(storageProvider).getJobById(jobInProgress.getId());

        assertThatCode(() -> task.saveAndRunJobFilters(singletonList(jobInProgress)))
                .isInstanceOf(SevereJobRunrException.class)
                .hasCauseInstanceOf(UnresolvableConcurrentJobModificationException.class);
    }

    @Test
    void ifNoStateChangeHappensStateChangeFiltersAreNotInvoked() {
        Job aJobInProgress = aJobInProgress().build();

        for (int i = 0; i <= 5; i++) {
            task.saveAndRunJobFilters(singletonList(aJobInProgress));
        }

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).isEmpty();
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(aJobInProgress)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(aJobInProgress)).isFalse();
    }

    @Test
    void ifStateChangeHappensStateChangeFiltersAreInvoked() {
        Job aJobInProgress = aJobInProgress().build();
        aJobInProgress.succeeded();

        task.saveAndRunJobFilters(singletonList(aJobInProgress));

        assertThat(logAllStateChangesFilter.getStateChanges(aJobInProgress)).containsExactly("PROCESSING->SUCCEEDED");
    }

    @Test
    void tasksArePostponedToNextRunIfPollIntervalInSecondsTimeboxIsAboutToPass() {
        reset(storageProvider);

        PeriodicTaskRunInfo periodicTaskRunInfo = zooKeeperStatistics.startRun(backgroundJobServer.getConfiguration());
        setRunStartTimeInPast(periodicTaskRunInfo, 15);

        task.run(periodicTaskRunInfo);

        verifyNoInteractions(storageProvider);
    }

    @Test
    void ifProcessToJobListHasNullItIsNotSaved() {
        // GIVEN
        List<Object> items = asList(null, null);
        Function<Object, Job> toJobFunction = x -> (Job) x;

        // WHEN
        task.convertAndProcessJobs(items, toJobFunction);

        // THEN
        verify(storageProvider, never()).save(anyList());
    }

    private static void setRunStartTimeInPast(PeriodicTaskRunInfo zooKeeperRunTaskInfo, int secondsInPast) {
        Whitebox.setInternalState(zooKeeperRunTaskInfo, "runStartTime", now().minusSeconds(secondsInPast));
    }
}