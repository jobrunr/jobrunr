package org.jobrunr.jobs;

import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;
import static org.jobrunr.jobs.JobTestBuilder.aSucceededJob;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

@ExtendWith(MockitoExtension.class)
class JobTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @Test
    void getJobIdentifier() {
        Job job = anEnqueuedJob().withJobDetails(systemOutPrintLnJobDetails("some message")).build();

        assertThat(job.getJobSignature()).isEqualTo("System.out.println(String)");
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
    void updateProcessingDoesNotThrowExceptionIfJobHasScheduledState() {
        Job job = aScheduledJob().withId().build();

        assertThatCode(job::updateProcessing).doesNotThrowAnyException();
    }

    @Test
    void updateProcessingDoesNotThrowExceptionIfJobHasSucceededState() {
        Job job = aSucceededJob().withId().build();

        assertThatCode(job::updateProcessing).doesNotThrowAnyException();
    }

}