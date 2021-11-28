package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.IllegalJobStateChangeException;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundStaticFieldJobWithoutIocRunner;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobPerformerTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private JobZooKeeper jobZooKeeper;

    private BackgroundJobTestFilter logAllStateChangesFilter;

    @BeforeEach
    void setUpMocks() {
        logAllStateChangesFilter = new BackgroundJobTestFilter();

        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getJobZooKeeper()).thenReturn(jobZooKeeper);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
    }

    @Test
    void onSuccessAfterDeleteTheIllegalJobStateChangeIsCatchedAndLogged() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> jobFromStorage.delete("for testing"));

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isTrue();
        assertThat(logger)
                .hasNoErrorLogMessages()
                .hasInfoMessage("Job finished successfully but it was already deleted - ignoring illegal state change from DELETED to SUCCEEDED");
    }

    @Test
    void onSuccessIllegalJobStateChangeIsThrown() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> jobFromStorage.failed("boe", new Exception()));

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        assertThatThrownBy(() -> backgroundJobPerformer.run())
                .isInstanceOf(IllegalJobStateChangeException.class);
    }

    @Test
    void onFailureAfterDeleteTheIllegalJobStateChangeIsCatchedAndLogged() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
            jobFromStorage.delete("for testing");
            jobFromStorage.delete("to throw exception that will bring it to failed state");
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
        assertThat(logger)
                .hasNoErrorLogMessages()
                .hasInfoMessage("Job processing failed but it was already deleted - ignoring illegal state change from DELETED to FAILED");
    }

    @Test
    void onFailureIllegalJobStateChangeIsThrown() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
            jobFromStorage.succeeded();
            jobFromStorage.succeeded(); //to throw exception that will bring it to failed state
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        assertThatThrownBy(() -> backgroundJobPerformer.run())
                .isInstanceOf(IllegalJobStateChangeException.class);
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(new BackgroundStaticFieldJobWithoutIocRunner());

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isTrue();
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnFailure() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED", "FAILED->SCHEDULED");
        assertThat(logAllStateChangesFilter.processingPassed).isTrue();
        assertThat(logAllStateChangesFilter.processedPassed).isFalse();
        assertThat(logger)
                .hasNoErrorLogMessages()
                .hasWarningMessageContaining("processing failed: An exception occurred during the performance of the job");
    }

    @Test
    void onFailureAfterAllRetriesExceptionIsLoggedToError() {
        Job job = aFailedJobWithRetries().withEnqueuedState(Instant.now()).build();
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.stateChanges).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED");
        assertThat(logger)
                .hasNoWarnLogMessages()
                .hasErrorMessage(String.format("Job(id=%s, jobName='failed job') processing failed: An exception occurred during the performance of the job", job.getId()));
    }

    @Test
    void onConcurrentJobModificationExceptionAllIsStillOk() throws Exception {
        Job job = anEnqueuedJob().build();

        when(storageProvider.save(job))
                .thenReturn(job)
                .thenThrow(new ConcurrentJobModificationException(job));
        mockBackgroundJobRunner(job, job1 -> {
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

    }

    @Test
    @DisplayName("InvocationTargetException is unwrapped and the actual error is stored instead")
    void invocationTargetExceptionUnwrapped() throws Exception {
        var job = anEnqueuedJob()
                .build();
        var runner = mock(BackgroundJobRunner.class);
        doThrow(new InvocationTargetException(new RuntimeException("test error"))).when(runner).run(job);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(runner);

        var backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        var lastFailure = job.getLastJobStateOfType(FailedState.class);
        assertThat(lastFailure.isPresent()).isTrue();
        assertThat(lastFailure.get().getExceptionMessage()).isEqualTo("test error");
        assertThat(lastFailure.get().getException()).isInstanceOf(RuntimeException.class);
        assertThat(lastFailure.get().getException().getMessage()).isEqualTo("test error");
    }

    @Test
    @DisplayName("any exception other than InvocationTargetException stays unwrapped")
    void anyExceptionOtherThanInvocationTargetExceptionIsNotUnwrapped() throws Exception {
        var job = anEnqueuedJob()
                .build();
        var runner = mock(BackgroundJobRunner.class);
        doThrow(new RuntimeException("test error")).when(runner).run(job);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(runner);

        var backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        var lastFailure = job.getLastJobStateOfType(FailedState.class);
        assertThat(lastFailure.isPresent()).isTrue();
        assertThat(lastFailure.get().getExceptionMessage()).isEqualTo("test error");
        assertThat(lastFailure.get().getException()).isInstanceOf(RuntimeException.class);
        assertThat(lastFailure.get().getException().getMessage()).isEqualTo("test error");
    }

    private void mockBackgroundJobRunner(Job job, Consumer<Job> jobConsumer) throws Exception {
        BackgroundJobRunner backgroundJobRunnerMock = mock(BackgroundJobRunner.class);
        doAnswer(invocation -> {
            final Job jobArgument = invocation.getArgument(0, Job.class);
            jobConsumer.accept(jobArgument);
            return null;
        }).when(backgroundJobRunnerMock).run(Mockito.any());
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(backgroundJobRunnerMock);
    }

}