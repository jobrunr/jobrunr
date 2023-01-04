package org.jobrunr.jobs;

import org.assertj.core.data.Offset;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.ProcessingState;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.SucceededState;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobDetailsTestBuilder.jobDetails;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
import static org.jobrunr.jobs.JobTestBuilder.*;
import static org.jobrunr.storage.BackgroundJobServerStatusTestBuilder.aDefaultBackgroundJobServerStatus;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JobTest {

    @Mock
    private BackgroundJobServer backgroundJobServer;

    @BeforeEach
    void setUpBackgroundJobServer() {
        lenient().when(backgroundJobServer.getServerStatus()).thenReturn(aDefaultBackgroundJobServerStatus().build());
    }

    @Test
    void ifIdIsNullAnIdIsCreated() {
        Job jobWithoutIdProvided = new Job(jobDetails().build());
        assertThat(jobWithoutIdProvided.getId()).isNotNull();

        Job jobWithNullIdProvided = new Job(null, jobDetails().build());
        assertThat(jobWithNullIdProvided.getId()).isNotNull();
    }

    @Test
    void getJobSignature() {
        Job job = anEnqueuedJob().withJobDetails(systemOutPrintLnJobDetails("some message")).build();

        assertThat(job.getJobSignature()).isEqualTo("java.lang.System.out.println(java.lang.String)");
    }

    @Test
    void jobCannotGoToProcessingTwice() {
        Job job = anEnqueuedJob().build();

        job.startProcessingOn(backgroundJobServer);
        assertThatThrownBy(() -> job.startProcessingOn(backgroundJobServer)).isInstanceOf(ConcurrentJobModificationException.class);
    }

    @Test
    void updateProcessingOnlyHasEffectIfJobIsInProcessingState() {
        Job job = anEnqueuedJob().build();
        job.startProcessingOn(backgroundJobServer);

        job.updateProcessing();

        ProcessingState processingState = job.getJobState();
        assertThat(processingState.getUpdatedAt()).isAfter(processingState.getCreatedAt());
    }

    @Test
    void updateProcessingThrowsExceptionIfJobHasScheduledState() {
        Job job = aScheduledJob().build();

        assertThatThrownBy(job::updateProcessing).isInstanceOf(ClassCastException.class);
    }

    @Test
    void updateProcessingThrowsExceptionIfJobHasSucceededState() {
        Job job = aSucceededJob().build();

        assertThatThrownBy(job::updateProcessing).isInstanceOf(ClassCastException.class);
    }

    @Test
    void succeededLatencyOnlyTakesIntoAccountStateFromEnqueuedToProcessing() {
        Job job = aJob()
                .withState(new ScheduledState(Instant.now()), Instant.now().minusSeconds(600))
                .withState(new EnqueuedState(), Instant.now().minusSeconds(60))
                .withState(new ProcessingState(backgroundJobServer), Instant.now().minusSeconds(10))
                .build();
        job.updateProcessing();
        job.succeeded();

        SucceededState succeededState = job.getJobState();
        assertThat(succeededState.getLatencyDuration().toSeconds()).isCloseTo(50, Offset.offset(1L));
        assertThat(succeededState.getProcessDuration().toSeconds()).isCloseTo(10, Offset.offset(1L));
    }

    @Test
    void metadataIsClearedWhenAJobSucceeds() {
        Job job = aJobInProgress().withMetadata("key", "value").build();
        assertThat(job).hasMetadata("key", "value");

        job.failed("En exception occurred", new RuntimeException("boem"));
        assertThat(job).hasMetadata("key", "value");

        job.scheduleAt(Instant.now(), "failure before");
        assertThat(job).hasMetadata("key", "value");

        job.succeeded();
        assertThat(job).hasNoMetadata();
    }

    @Test
    void metadataIsClearedWhenAJobIsDeleted() {
        Job job = aJobInProgress().withMetadata("key", "value").build();
        assertThat(job).hasMetadata("key", "value");

        job.failed("En exception occurred", new RuntimeException("boem"));
        assertThat(job).hasMetadata("key", "value");

        job.scheduleAt(Instant.now(), "failure before");
        assertThat(job).hasMetadata("key", "value");

        job.delete("From UI");
        assertThat(job).hasNoMetadata();
    }

    @Test
    void jobLoggingAndProgressIsNotClearedIfMoreThan10Retries() {
        Job job = anEnqueuedJob().build();
        for(int i = 0; i < 10; i++) {
            job.startProcessingOn(backgroundJobServer);

            JobDashboardLogger jobDashboardLogger = new JobDashboardLogger(job);
            jobDashboardLogger.info("Message " + i);

            job.failed("Job failed", new IllegalStateException("Not important"));
            job.scheduleAt(Instant.now(), "Retry");
            job.enqueue();
        }

        job.startProcessingOn(backgroundJobServer);
        job.succeeded();
        assertThat(job)
                .hasMetadata("jobRunrDashboardLog-2")
                .hasMetadata("jobRunrDashboardLog-6")
                .hasMetadata("jobRunrDashboardLog-10")
                .hasMetadata("jobRunrDashboardLog-14")
                .hasMetadata("jobRunrDashboardLog-18")
                .hasMetadata("jobRunrDashboardLog-22")
                .hasMetadata("jobRunrDashboardLog-26")
                .hasMetadata("jobRunrDashboardLog-30")
                .hasMetadata("jobRunrDashboardLog-34")
                .hasMetadata("jobRunrDashboardLog-38");
    }
}