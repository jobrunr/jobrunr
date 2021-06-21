package org.jobrunr.server.runner;

import org.jobrunr.jobs.Job;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobTestBuilder.aJobInProgress;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AbstractBackgroundJobRunnerTest {

    @Mock
    private AbstractBackgroundJobRunner.BackgroundJobWorker worker;

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

    private AbstractBackgroundJobRunner getJobRunner() {
        return new AbstractBackgroundJobRunner() {
            @Override
            public boolean supports(Job job) {
                return true;
            }

            @Override
            protected BackgroundJobWorker getBackgroundJobWorker(Job job) {
                return worker;
            }
        };
    }

}