package org.jobrunr.jobs;

import org.assertj.core.data.Offset;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aJob;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

@ExtendWith(MockitoExtension.class)
class JobTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @Test
    void getJobSignature() {
        Job job = anEnqueuedJob().withJobDetails(systemOutPrintLnJobDetails("some message")).build();

        assertThat(job.getJobSignature()).isEqualTo("java.lang.System.out.println(java.lang.String)");
    }

    @Test
    void jobCannotGoToProcessingTwice() {
        Job job = anEnqueuedJob().withId().build();

        job.startProcessingOn(backgroundJobServer);
        assertThatThrownBy(() -> job.startProcessingOn(backgroundJobServer)).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    void updateProcessingOnlyHasEffectIfJobIsInProcessingState() {
        Job job = anEnqueuedJob().withId().build();
        job.startProcessingOn(backgroundJobServer);

        job.updateProcessing();

        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void updateProcessingThrowsExceptionIfJobHasScheduledState() {
        Job job = aScheduledJob().withId().build();

        assertThatThrownBy(job::updateProcessing).isInstanceOf(ClassCastException.class);
    }

    @Test
    void updateProcessingThrowsExceptionIfJobHasSucceededState() {
        Job job = aSucceededJob().withId().build();

        assertThatThrownBy(job::updateProcessing).isInstanceOf(ClassCastException.class);
    }

    @Test
    void succeededLatencyOnlyTakesIntoAccountStateFromEnqueuedToProcessing() {
        Job job = aJob()
                .withState(new ScheduledState(Instant.now()), Instant.now().minusSeconds(600))
                .withState(new EnqueuedState(), Instant.now().minusSeconds(60))
                .withState(new ProcessingState(backgroundJobServer.getId()), Instant.now().minusSeconds(10))
                .build();
        job.updateProcessing();
        job.succeeded();

        SucceededState succeededState = job.getJobState();
        assertThat(succeededState.getLatencyDuration().toSeconds()).isCloseTo(50, Offset.offset(1L));
        assertThat(succeededState.getProcessDuration().toSeconds()).isCloseTo(10, Offset.offset(1L));
    }
}