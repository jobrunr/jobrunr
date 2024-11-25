package org.jobrunr.server;

import ch.qos.logback.LoggerAssert;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.jobrunr.JobRunrException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.JobDefaultFilters;
import org.jobrunr.jobs.mappers.MDCMapper;
import org.jobrunr.jobs.states.FailedState;
import org.jobrunr.jobs.states.IllegalJobStateChangeException;
import org.jobrunr.server.runner.BackgroundJobRunner;
import org.jobrunr.server.runner.BackgroundStaticFieldJobWithoutIocRunner;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.stubs.Mocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aFailedJobWithRetries;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;
import static org.jobrunr.jobs.states.StateName.FAILED;
import static org.jobrunr.jobs.states.StateName.SCHEDULED;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackgroundJobPerformerTest {

    private BackgroundJobServer backgroundJobServer = Mocks.ofBackgroundJobServer();
    @Mock
    private StorageProvider storageProvider;
    @Mock
    private JobSteward jobSteward;

    private LogAllStateChangesFilter logAllStateChangesFilter;

    @BeforeEach
    void setUpMocks() {
        logAllStateChangesFilter = new LogAllStateChangesFilter();

        when(backgroundJobServer.getStorageProvider()).thenReturn(storageProvider);
        when(backgroundJobServer.getJobSteward()).thenReturn(jobSteward);
        when(backgroundJobServer.getJobFilters()).thenReturn(new JobDefaultFilters(logAllStateChangesFilter));
    }

    @Test
    void onStartIfJobIsProcessingByStorageProviderItStaysInProcessingAndThenSucceeded() throws Exception {
        Job job = anEnqueuedJob()
                .withProcessingState(backgroundJobServer.getConfiguration().getId())
                .build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isTrue();
        assertThat(logger)
                .hasNoErrorLogMessages();
    }

    @Test
    void onStartIfJobIsNotProcessingByStorageProviderItGoesToProcessingAndThenSucceeded() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isTrue();
        assertThat(logger)
                .hasNoErrorLogMessages();
    }

    @Test
    void onSuccessAfterDeleteTheIllegalJobStateChangeIsCatchedAndLogged() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> jobFromStorage.delete("for testing"));

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isTrue();
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

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isFalse();
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
    void onInterruptedExceptionThreadIsInterruptedAndJobIsRescheduled() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
            throw new JobRunrException("Thread interrupted", new InterruptedException());
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(job.getJobState(-1))
                .hasFieldOrPropertyWithValue("state", SCHEDULED)
                .hasFieldOrPropertyWithValue("reason", "Retry 1 of 10");

        assertThat(job.getJobState(-2))
                .hasFieldOrPropertyWithValue("state", FAILED)
                .hasFieldOrPropertyWithValue("message", "Job processing was stopped as background job server has stopped");
    }

    @Test
    void onJobActivatorShutdownExceptionThreadIsInterruptedAndJobIsRescheduled() throws Exception {
        Job job = anEnqueuedJob().build();

        mockBackgroundJobRunner(job, jobFromStorage -> {
            throw new JobActivatorShutdownException("The JobActivator is in shutdown", new Exception());
        });

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(job.getJobState(-1))
                .hasFieldOrPropertyWithValue("state", SCHEDULED)
                .hasFieldOrPropertyWithValue("reason", "Retry 1 of 10");

        assertThat(job.getJobState(-2))
                .hasFieldOrPropertyWithValue("state", FAILED)
                .hasFieldOrPropertyWithValue("message", "Job processing was stopped as background job server has stopped");
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnSuccess() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(new BackgroundStaticFieldJobWithoutIocRunner());

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->SUCCEEDED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingFailedIsCalled(job)).isFalse();
    }

    @Test
    void allStateChangesArePassingViaTheApplyStateFilterOnFailure() {
        Job job = anEnqueuedJob().build();

        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED", "FAILED->SCHEDULED");
        assertThat(logAllStateChangesFilter.onProcessingIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingFailedIsCalled(job)).isTrue();
        assertThat(logAllStateChangesFilter.onProcessingSucceededIsCalled(job)).isFalse();

        assertThat(logger)
                .hasNoErrorLogMessages()
                .hasWarningMessageContaining("processing failed: An exception occurred during the performance of the job");
    }

    @Test
    void onFailureAndRetriesAreNotExhaustedServerFilterOnFailedAfterRetriesIsNotCalled() {
        Job job = anEnqueuedJob().build();
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.onFailedAfterRetriesIsCalled(job)).isFalse();
    }

    @Test
    void onFailureAfterAllRetriesServerFilterOnFailedAfterRetriesNotCalled() {
        Job job = aFailedJobWithRetries().withEnqueuedState(Instant.now()).build();
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.onFailedAfterRetriesIsCalled(job)).isTrue();
    }

    @Test
    void onFailureAfterAllRetriesExceptionIsLoggedToError() {
        Job job = aFailedJobWithRetries().withEnqueuedState(Instant.now()).build();
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(null);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        final ListAppender<ILoggingEvent> logger = LoggerAssert.initFor(backgroundJobPerformer);
        backgroundJobPerformer.run();

        assertThat(logAllStateChangesFilter.getStateChanges(job)).containsExactly("ENQUEUED->PROCESSING", "PROCESSING->FAILED");
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
        var job = anEnqueuedJob().build();
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
        var job = anEnqueuedJob().build();
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

    @Test
    void mdcIsAlsoAvailableDuringLoggingOfJobSuccess() throws Exception {
        // GIVEN
        Job job = anEnqueuedJob().build();
        MDC.put("testKey", "testValue");
        MDCMapper.saveMDCContextToJob(job);

        BackgroundJobRunner runner = mock(BackgroundJobRunner.class);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(runner);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        ListAppender logger = LoggerAssert.initFor(backgroundJobPerformer);

        // WHEN
        backgroundJobPerformer.run();

        // THEN
        assertThat(logger)
                .hasDebugMessageContaining(
                        "Job(id=" + job.getId() + ", jobName='" + job.getJobName() + "') processing succeeded",
                        Map.of(
                                "jobrunr.jobId", job.getId().toString(),
                                "jobrunr.jobName", job.getJobName(),
                                "jobrunr.jobSignature", job.getJobSignature(),
                                "testKey", "testValue"
                        ));

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); // backgroundJobPerformer clears MDC Context
    }

    @Test
    void mdcIsAlsoAvailableDuringLoggingOfJobFailure() throws Exception {
        // GIVEN
        Job job = anEnqueuedJob().build();
        MDC.put("testKey", "testValue");
        MDCMapper.saveMDCContextToJob(job);

        BackgroundJobRunner runner = mock(BackgroundJobRunner.class);
        doThrow(new InvocationTargetException(new RuntimeException("test error"))).when(runner).run(job);
        when(backgroundJobServer.getBackgroundJobRunner(job)).thenReturn(runner);

        BackgroundJobPerformer backgroundJobPerformer = new BackgroundJobPerformer(backgroundJobServer, job);
        ListAppender logger = LoggerAssert.initFor(backgroundJobPerformer);

        // WHEN
        backgroundJobPerformer.run();

        // THEN
        assertThat(logger)
                .hasWarningMessageContaining(
                        "Job(id=" + job.getId() + ", jobName='" + job.getJobName() + "') processing failed",
                        Map.of(
                                "jobrunr.jobId", job.getId().toString(),
                                "jobrunr.jobName", job.getJobName(),
                                "jobrunr.jobSignature", job.getJobSignature(),
                                "testKey", "testValue"
                        ));
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty(); // backgroundJobPerformer clears MDC Context
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