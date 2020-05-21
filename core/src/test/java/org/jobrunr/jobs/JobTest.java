package org.jobrunr.jobs;

import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.storage.ConcurrentJobModificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jobrunr.jobs.JobDetailsTestBuilder.systemOutPrintLnJobDetails;
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
    void increaseVersion() {
        Job job = anEnqueuedJob().withId().build();

        assertThat(job.increaseVersion()).isEqualTo(0);
        assertThat(job.getVersion()).isEqualTo(1);

        assertThat(job.increaseVersion()).isEqualTo(1);
        assertThat(job.getVersion()).isEqualTo(2);
    }

}