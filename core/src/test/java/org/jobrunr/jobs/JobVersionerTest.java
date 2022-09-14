package org.jobrunr.jobs;

import org.junit.jupiter.api.Test;

import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;

class JobVersionerTest {

    @Test
    void testJobVersionerOnCommitVersionIsIncreased() {
        Job job = aScheduledJob().withVersion(5).build();
        JobVersioner jobVersioner = new JobVersioner(job);

        jobVersioner.commitVersion();

        assertThat(job).hasVersion(6);
    }

    @Test
    void testJobVersionerOnRollbackVersionIsRestored() {
        Job job = aScheduledJob().withVersion(5).build();
        JobVersioner jobVersioner = new JobVersioner(job);

        jobVersioner.close();

        assertThat(job).hasVersion(5);
    }

    @Test
    void testJobVersionerInTryWithResourcesOnRollbackVersionIsRestored() {
        Job job = aScheduledJob().withVersion(5).build();
        try(JobVersioner jobVersioner = new JobVersioner(job)) {
            // nothing to do as not committed
        }

        assertThat(job).hasVersion(5);
    }

}