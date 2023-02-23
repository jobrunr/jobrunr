package org.jobrunr.server.zookeeper.tasks;

import org.jobrunr.jobs.Job;
import org.jobrunr.stubs.TestServiceInterface;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.emptyJobList;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DeleteSucceededJobsTaskTest extends AbstractZooKeeperTaskTest {

    DeleteSucceededJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new DeleteSucceededJobsTask(jobZooKeeper, backgroundJobServer);
    }

    @Test
    void testTask() {
        when(storageProvider.getJobs(eq(SUCCEEDED), any(), any())).thenReturn(asList(aSucceededJob().build(), aSucceededJob().build()), emptyJobList());
        runTask(task);

        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(2);

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("SUCCEEDED->DELETED", "SUCCEEDED->DELETED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled).isFalse();
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/27")
    void taskMovesSucceededJobsToDeletedStateAlsoForMethodsThatDontExistAnymore() {
        Job job = aSucceededJob().withJobDetails(methodThatDoesNotExistJobDetails()).build();

        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        singletonList(job),
                        emptyJobList());

        // WHEN
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(1);
    }

    @Test
    void taskMovesSucceededJobsToDeletedStateAlsoForInterfacesWithMethodsThatDontExistAnymore() {
        // GIVEN
        lenient().when(storageProvider.getJobs(eq(SUCCEEDED), any(Instant.class), any()))
                .thenReturn(
                        asList(aSucceededJob()
                                .withJobDetails(jobDetails()
                                        .withClassName(TestServiceInterface.class)
                                        .withMethodName("methodThatDoesNotExist")
                                        .build())
                                .build()),
                        emptyJobList()
                );

        // WHEN
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(1);
    }
}