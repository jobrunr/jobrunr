package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.jobrunr.server.runner.AbstractBackgroundJobRunner.BackgroundJobWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AbstractBackgroundJobRunnerTest {

    @Mock
    private BackgroundJobWorker worker;

    @Test
    void ifCodeThrowsInterruptedException_AnInterruptExceptionIsThrown() throws Exception {
        final AbstractBackgroundJobRunner backgroundJobRunner = getJobRunner();
        final Job job = aJobInProgress().build();
        doThrow(new InterruptedException()).when(worker).run();

        assertThatThrownBy(() -> backgroundJobRunner.run(job)).isInstanceOf(InterruptedException.class);
    }

    @Test
    void ifCurrentThreadIsInterrupted_AnInterruptExceptionIsThrown() throws Exception {
        final AbstractBackgroundJobRunner backgroundJobRunner = getJobRunner();
        final Job job = aJobInProgress().build();
        doAnswer(invocation -> {
            Thread.currentThread().interrupt();
            return null;
        }).when(worker).run();

        assertThatThrownBy(() -> backgroundJobRunner.run(job)).isInstanceOf(InterruptedException.class);
    }

    @Test
    void invokeJobMethodUpdatesJobContextThreadLocal() throws Exception {
        final Job job = aJobInProgress().withJobDetails(this::testJobContext).build();

        final AbstractBackgroundJobRunner backgroundJobRunner = getJobRunner(BackgroundJobWorker::new);

        backgroundJobRunner.run(job);
        assertThat(ThreadLocalJobContext.getJobContext()).isNull();
    }

    @Test
    void invokeJobMethodAlwaysResetsJobContextThreadLocal() {
        final Job job = aJobInProgress().withJobDetails(this::throwingJobContext).build();

        final AbstractBackgroundJobRunner backgroundJobRunner = getJobRunner(BackgroundJobWorker::new);

        assertThatCode(() -> backgroundJobRunner.run(job)).isInstanceOf(Exception.class);
        assertThat(ThreadLocalJobContext.getJobContext()).isNull();
    }

    private AbstractBackgroundJobRunner getJobRunner() {
        return getJobRunner(job -> worker);
    }

    private AbstractBackgroundJobRunner getJobRunner(Function<Job, BackgroundJobWorker> worker) {
        return new AbstractBackgroundJobRunner() {
            @Override
            public boolean supports(Job job) {
                return true;
            }

            @Override
            protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
                return worker.apply(job);
            }
        };
    }

    public void testJobContext() {
        assertThat(ThreadLocalJobContext.getJobContext()).isNotNull();
    }

    public void throwingJobContext() {
        assertThat(ThreadLocalJobContext.getJobContext()).isNotNull();
        throw new RuntimeException("Boem");
    }

}