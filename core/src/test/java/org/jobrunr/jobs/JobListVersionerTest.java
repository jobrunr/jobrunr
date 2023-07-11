package org.jobrunr.jobs;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.jobrunr.JobRunrAssertions.assertThat;
import static org.jobrunr.jobs.JobTestBuilder.aScheduledJob;

class JobListVersionerTest {

    @Test
    void testJobListVersionerOnCommitVersionIsIncreased() {
        // GIVEN
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();

        // WHEN
        JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2));
        jobListVersioner.commitVersions();

        // THEN
        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(6);
    }

    @Test
    void testJobListVersionerOnRollbackOfSomeJobsVersionIsDecreasedForThoseJobs() {
        // GIVEN
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();

        // WHEN
        JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2));
        jobListVersioner.rollbackVersions(asList(job2));
        jobListVersioner.close();

        // THEN
        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(5);
    }

    @Test
    void testJobListVersionerInTryWithResourcesOnRollbackOfSomeJobsVersionIsDecreasedForThoseJobs() {
        // GIVEN
        Job job1 = aScheduledJob().withVersion(5).build();
        Job job2 = aScheduledJob().withVersion(5).build();

        // WHEN
        try (JobListVersioner jobListVersioner = new JobListVersioner(asList(job1, job2))) {
            jobListVersioner.rollbackVersions(asList(job2));
        }

        // THEN
        assertThat(job1).hasVersion(6);
        assertThat(job2).hasVersion(5);
    }
}