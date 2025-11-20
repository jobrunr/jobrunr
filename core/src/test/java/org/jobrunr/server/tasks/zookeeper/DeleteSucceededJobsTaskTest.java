package org.jobrunr.server.tasks.zookeeper;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.server.tasks.AbstractTaskTest;
import org.jobrunr.stubs.TestServiceInterface;
import org.jobrunr.utils.annotations.Because;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.within;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.methodThatDoesNotExistJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.states.StateName.SUCCEEDED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

class DeleteSucceededJobsTaskTest extends AbstractTaskTest {

    DeleteSucceededJobsTask task;

    @BeforeEach
    void setUpTask() {
        task = new DeleteSucceededJobsTask(backgroundJobServer);
    }

    @Override
    protected void setUpBackgroundJobServerConfiguration(BackgroundJobServerConfiguration configuration) {
        configuration.andDeleteSucceededJobsAfter(Duration.ofDays(2));
    }

    @Test
    void testTask() {
        Job succeededJob1 = aSucceededJob().build();
        Job succeededJob2 = aSucceededJob().build();
        whenGetJobsToDeleteThenReturn(List.of(succeededJob1, succeededJob2));
        runTask(task);

        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(2);

        assertThat(logAllStateChangesFilter.getStateChanges(succeededJob1)).containsExactly("SUCCEEDED->DELETED");
        assertThat(logAllStateChangesFilter.getStateChanges(succeededJob2)).containsExactly("SUCCEEDED->DELETED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(succeededJob1)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(succeededJob2)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(succeededJob1)).isFalse();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(succeededJob2)).isFalse();
    }

    @Test
    void testTaskTakesIntoAccountConfiguration() {
        runTask(task);

        verify(storageProvider).getJobList(eq(SUCCEEDED), assertArg(x -> assertThat(x).isCloseTo(now().minus(Duration.ofDays(2)), within(5, SECONDS))), any());
    }

    @Test
    @Because("https://github.com/jobrunr/jobrunr/issues/27")
    void taskMovesSucceededJobsToDeletedStateAlsoForMethodsThatDontExistAnymore() {
        Job job = aSucceededJob().withJobDetails(methodThatDoesNotExistJobDetails()).build();

        whenGetJobsToDeleteThenReturn(singletonList(job));

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
        Job job = aSucceededJob().withJobDetails(jobDetails()
                        .withClassName(TestServiceInterface.class)
                        .withMethodName("methodThatDoesNotExist")
                        .build())
                .build();

        whenGetJobsToDeleteThenReturn(singletonList(job));

        // WHEN
        runTask(task);

        // THEN
        assertThat(logger).hasNoWarnLogMessages();
        verify(storageProvider).save(anyList());
        verify(storageProvider).publishTotalAmountOfSucceededJobs(1);
    }

    private void whenGetJobsToDeleteThenReturn(List<Job> jobs) {
        doReturn(jobs, Collections.<Job>emptyList())
                .when(storageProvider)
                .getJobList(eq(SUCCEEDED), any(Instant.class), any());
    }
}