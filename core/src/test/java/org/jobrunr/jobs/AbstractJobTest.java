package org.jobrunr.jobs;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.anEnqueuedJob;

class AbstractJobTest {

    @Test
    void increaseVersion() {
        Job job = anEnqueuedJob().withId().build();

        assertThat(job.increaseVersion()).isEqualTo(0);
        assertThat(job.getVersion()).isEqualTo(1);

        assertThat(job.increaseVersion()).isEqualTo(1);
        assertThat(job.getVersion()).isEqualTo(2);
    }

}